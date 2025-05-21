package com.example.shelldemo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.parser.SqlScriptParser.ProcedureParam;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.parser.SqlStatementFactory;
import com.example.shelldemo.config.ConfigHolderAdapter;
import com.example.shelldemo.config.DatabaseConfig;
import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.ParseException;
import com.example.shelldemo.executor.DdlExecutor;
import com.example.shelldemo.executor.DmlExecutor;
import com.example.shelldemo.executor.ProcedureExecutor;
import com.example.shelldemo.executor.QueryExecutor;
import com.example.shelldemo.executor.SqlExecutor;
import com.example.shelldemo.spi.DatabaseVendor;
import com.example.shelldemo.transaction.TransactionManager;

/**
 * Enhanced facade for unified database operations.
 * Delegates to specialized executor components for different SQL operation types.
 */
public class UnifiedDatabaseOperation implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseOperation.class);
    private static final Logger resultLogger = LogManager.getLogger("com.example.shelldemo.resultset");

    private final DatabaseContext context;
    private final SqlStatementFactory statementFactory;
    private final QueryExecutor queryExecutor;
    private final DmlExecutor dmlExecutor;
    private final DdlExecutor ddlExecutor;
    private final ProcedureExecutor procedureExecutor;
    private final DatabaseVendor vendor;
    private final boolean transactional;

    /**
     * Creates a new UnifiedDatabaseOperation with the provided context.
     * This constructor is package-private to encourage use of the builder.
     *
     * @param context The database context
     * @param vendor The DatabaseVendor implementation
     * @param transactional The transactional flag
     */
    UnifiedDatabaseOperation(DatabaseContext context, DatabaseVendor vendor, boolean transactional) {
        logger.debug("Entering UnifiedDatabaseOperation constructor with vendor: {}", vendor != null ? vendor.getVendorName() : null);
        this.context = context;
        this.vendor = vendor;
        this.transactional = transactional;
        
        // Create executor components
        this.queryExecutor = new QueryExecutor(context);
        this.dmlExecutor = new DmlExecutor(context);
        this.ddlExecutor = new DdlExecutor(context);
        this.procedureExecutor = new ProcedureExecutor(context);
        
        // Create statement factory - using context's built-in vendor
        // DatabaseContext adapts the vendor internally
        this.statementFactory = new SqlStatementFactory(
            Map.of(context.getDbType(), context.getVendor())
        );
        
        logger.debug("UnifiedDatabaseOperation created for database type: {}", context.getDbType());
        logger.debug("Exiting UnifiedDatabaseOperation constructor");
    }

    /**
     * Factory method to create a UnifiedDatabaseOperation from a DatabaseConfig.
     *
     * @param config The database configuration
     * @param vendor The DatabaseVendor implementation
     * @param transactional The transactional flag
     * @return A new UnifiedDatabaseOperation
     */
    public static UnifiedDatabaseOperation create(DatabaseConfig config, DatabaseVendor vendor, boolean transactional) {
        try {
            // Create a connection
            var factory = new com.example.shelldemo.connection.DatabaseConnectionFactory();
            var conn = factory.createConnection(
                new com.example.shelldemo.connection.ConnectionConfig(
                    config.host(),
                    config.port(),
                    config.username(),
                    config.password(),
                    config.database(),
                    config.vendor(),
                    config.connectionType()
                )
            );
            
            // Create the context - use the Builder to adapt the vendor
            DatabaseContext context = new DatabaseContext.Builder()
                .connection(conn)
                .dbType(config.vendor())
                .vendor(vendor) // This will use the adapter in the Builder
                .configService(new ConfigHolderAdapter())
                .build();
            
            // Create and return the operation
            return new UnifiedDatabaseOperation(context, vendor, transactional);
            
        } catch (SQLException e) {
            logger.error("Failed to create database connection: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create database connection", e, ErrorType.CONN_FAILED);
        }
    }
    
    /**
     * Executes a SQL query and returns the results as a list of maps.
     *
     * @param sql The SQL query
     * @return A list of result rows as maps
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        logger.debug("Entering executeQuery with SQL: {}", sql);
        SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
        if (!(statement instanceof SqlStatement.QueryStatement)) {
            throw new IllegalArgumentException("Expected a query statement but got: " + statement.getClass().getSimpleName());
        }
        List<Map<String, Object>> result = (List<Map<String, Object>>) queryExecutor.execute(statement);
        logger.debug("Exiting executeQuery with result: {}", result);
        return result;
    }
    
    /**
     * Executes a SQL update statement and returns the number of affected rows.
     *
     * @param sql The SQL update statement
     * @return The number of affected rows
     * @throws SQLException if a database access error occurs
     */
    public int executeUpdate(String sql) throws SQLException {
        logger.debug("Entering executeUpdate with SQL: {}", sql);
        SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
        if (!(statement instanceof SqlStatement.DmlStatement)) {
            throw new IllegalArgumentException("Expected a DML statement but got: " + statement.getClass().getSimpleName());
        }
        int result = (Integer) dmlExecutor.execute(statement);
        logger.debug("Exiting executeUpdate with result: {}", result);
        return result;
    }
    
    /**
     * Executes a DDL statement.
     *
     * @param sql The DDL statement
     * @return true if the statement was executed successfully
     * @throws SQLException if a database access error occurs
     */
    public boolean executeDdl(String sql) throws SQLException {
        logger.debug("Entering executeDdl with SQL: {}", sql);
        SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
        if (!(statement instanceof SqlStatement.DdlStatement)) {
            throw new IllegalArgumentException("Expected a DDL statement but got: " + statement.getClass().getSimpleName());
        }
        boolean result = (Boolean) ddlExecutor.execute(statement);
        logger.debug("Exiting executeDdl with result: {}", result);
        return result;
    }
    
    /**
     * Executes a stored procedure or PL/SQL block.
     *
     * @param sql The procedure or PL/SQL block
     * @return A map of output parameters and their values
     * @throws SQLException if a database access error occurs
     */
    public Map<String, Object> executeProcedure(String sql) throws SQLException {
        logger.debug("Entering executeProcedure with SQL: {}", sql);
        SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
        if (!(statement instanceof SqlStatement.ProcedureStatement)) {
            throw new IllegalArgumentException("Expected a procedure statement but got: " + statement.getClass().getSimpleName());
        }
        Map<String, Object> result = (Map<String, Object>) procedureExecutor.execute(statement);
        logger.debug("Exiting executeProcedure with result: {}", result);
        return result;
    }
    
    /**
     * Calls a stored procedure with parameters.
     *
     * @param procedureName The name of the procedure
     * @param inParams Input parameters
     * @param outParams Output parameters
     * @return A map of output parameters and their values
     * @throws SQLException if a database access error occurs
     */
    public Map<String, Object> callStoredProcedure(
            String procedureName, 
            List<ProcedureParam> inParams,
            List<ProcedureParam> outParams) throws SQLException {
        logger.debug("Entering callStoredProcedure with procedureName: {}, inParams: {}, outParams: {}", procedureName, inParams, outParams);
        Map<String, Object> result = procedureExecutor.callProcedure(procedureName, inParams, outParams);
        logger.debug("Exiting callStoredProcedure with result: {}", result);
        return result;
    }
    
    private static String formatTable(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return "(no results)";
        var columns = rows.get(0).keySet().toArray(new String[0]);
        int[] widths = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            widths[i] = columns[i].length();
        }
        for (Map<String, Object> row : rows) {
            int i = 0;
            for (String col : columns) {
                Object val = row.get(col);
                if (val != null) {
                    widths[i] = Math.max(widths[i], val.toString().length());
                }
                i++;
            }
        }
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        sb.append("\n|");
        for (int i = 0; i < columns.length; i++) {
            sb.append(" ").append(String.format("%-" + widths[i] + "s", columns[i])).append(" |");
        }
        sb.append("\n+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        sb.append("\n");
        // Rows
        for (Map<String, Object> row : rows) {
            sb.append("|");
            int i = 0;
            for (String col : columns) {
                Object val = row.get(col);
                sb.append(" ").append(String.format("%-" + widths[i] + "s", val != null ? val : "")).append(" |");
                i++;
            }
            sb.append("\n");
        }
        sb.append("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        return sb.toString();
    }

    /**
     * Executes a SQL script file.
     *
     * @param scriptFile The script file
     * @return The number of statements executed
     * @throws SQLException if a database access error occurs
     * @throws IOException if an I/O error occurs
     */
    public int executeScript(File scriptFile) throws SQLException, IOException {
        String user = null;
        try { user = context.getConnection().getMetaData().getUserName(); } catch (Exception ignore) {}
        ThreadContext.put("operation", "executeScript");
        ThreadContext.put("scriptFile", scriptFile.getName());
        ThreadContext.put("dbType", context.getDbType());
        if (user != null) ThreadContext.put("user", user);
        try {
            logger.debug("Entering executeScript with file: {}", scriptFile);
            try {
                Map<Integer, String> statements = SqlScriptParser.parseSqlFile(scriptFile, getVendor());
                logger.debug("Parsed {} SQL statements from script.", statements.size());
                logger.info("About to execute {} SQL statements from script: {}", statements.size(), scriptFile);
                int executed = 0;
                for (String sql : statements.values()) {
                    logger.debug("Preparing to execute SQL statement: {}", sql);
                    SqlStatement stmt = statementFactory.createStatement(sql, context.getDbType());
                    SqlExecutor executor = SqlExecutor.createFor(stmt, context);
                    // DML: use transaction if requested
                    if (stmt instanceof com.example.shelldemo.parser.SqlStatement.DmlStatement && transactional) {
                        logger.debug("Executing DML statement in transaction: {}", sql);
                        TransactionManager txManager = context.getTransactionManager();
                        Object execResult = txManager.executeInTransaction(conn -> executor.execute(stmt));
                        logger.debug("Result for DML statement: {} => {}", sql, execResult);
                        if (execResult instanceof java.util.List<?> results && !results.isEmpty() && results.get(0) instanceof Map) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> tableRows = (List<Map<String, Object>>) results;
                            resultLogger.info("\n" + formatTable(tableRows));
                        }
                    } else {
                        logger.debug("Executing statement (auto-commit): {}", sql);
                        Object execResult = executor.execute(stmt);
                        logger.debug("Result for statement: {} => {}", sql, execResult);
                        if (execResult instanceof java.util.List<?> results && !results.isEmpty() && results.get(0) instanceof Map) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> tableRows = (List<Map<String, Object>>) results;
                            resultLogger.info("\n" + formatTable(tableRows));
                        }
                    }
                    executed++;
                }
                logger.info("Executed {} SQL statements from script: {}", executed, scriptFile);
                logger.debug("Exiting executeScript with executed count: {}", executed);
                return executed;
            } catch (ParseException e) {
                throw new DatabaseException("Failed to parse SQL script: " + scriptFile.getName(), 
                        e, ErrorType.PARSE_SQL);
            } finally {
                logger.debug("Exiting executeScript");
            }
        } finally {
            ThreadContext.clearAll();
        }
    }
    
    /**
     * Gets the database context.
     *
     * @return The database context
     */
    public DatabaseContext getContext() {
        return context;
    }
    
    /**
     * Gets the database vendor.
     *
     * @return The database vendor
     */
    public DatabaseVendor getVendor() {
        return vendor;
    }
    
    /**
     * Gets the connection URL using the vendor-specific format.
     *
     * @param host The host
     * @param port The port
     * @param database The database name
     * @param connectionType The connection type
     * @return The connection URL
     */
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        return vendor.buildConnectionUrl(host, port, database, connectionType);
    }
    
    @Override
    public void close() throws Exception {
        context.close();
        logger.debug("UnifiedDatabaseOperation closed");
    }
}
