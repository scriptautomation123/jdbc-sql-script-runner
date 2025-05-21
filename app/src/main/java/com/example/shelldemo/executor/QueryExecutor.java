package com.example.shelldemo.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.sqlhandling.ResultSetProcessor;
import com.example.shelldemo.transaction.TransactionManager;
import com.example.shelldemo.util.ExceptionUtils;
import com.example.shelldemo.util.LoggingUtils;

/**
 * Executor for SQL query statements that return result sets.
 */
public final class QueryExecutor implements SqlExecutor {
    private final DatabaseContext context;
    private final ResultSetProcessor resultProcessor;
    
    /**
     * Creates a new QueryExecutor.
     *
     * @param context The database context
     */
    public QueryExecutor(DatabaseContext context) {
        this.context = context;
        this.resultProcessor = new ResultSetProcessor();
    }
    
    @Override
    public List<Map<String, Object>> execute(SqlStatement statement) throws SQLException {
        Logger logger = context.getLogger();
        String user = null;
        try { user = context.getConnection().getMetaData().getUserName(); } catch (Exception ignore) {}
        ThreadContext.put("operation", "query");
        ThreadContext.put("dbType", context.getDbType());
        if (user != null) ThreadContext.put("user", user);
        logger.debug("Entering QueryExecutor.execute with SQL: {}", statement.getText());
        if (!(statement instanceof SqlStatement.QueryStatement)) {
            throw new IllegalArgumentException("Expected QueryStatement but got " + statement.getClass().getSimpleName());
        }
        String sql = statement.getText();
        Connection connection = context.getConnection();
        LoggingUtils.logOperation(logger, "query", sql);
        LoggingUtils.logSql(logger, sql);
        try {
            List<Map<String, Object>> result = executeQuery(sql, connection);
            logger.debug("Exiting QueryExecutor.execute with result: {}", result);
            return result;
        } catch (SQLException e) {
            throw ExceptionUtils.handleSQLException(
                e, "execute query", ErrorType.OP_QUERY, logger);
        } finally {
            ThreadContext.clearAll();
        }
    }
    
    /**
     * Executes a query and processes the results.
     *
     * @param sql The SQL query
     * @param connection The database connection
     * @return A list of result rows as maps
     * @throws SQLException if a database access error occurs
     */
    private List<Map<String, Object>> executeQuery(String sql, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return resultProcessor.processResultSet(rs);
        }
    }
    
    /**
     * Executes a query within a transaction.
     *
     * @param sql The SQL query
     * @return A list of result rows as maps
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> executeInTransaction(String sql) throws SQLException {
        TransactionManager txManager = context.getTransactionManager();
        return txManager.executeInTransaction(conn -> executeQuery(sql, conn));
    }
    
    @Override
    public DatabaseContext getContext() {
        return context;
    }
}
