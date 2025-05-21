package com.example.shelldemo.sqlhandling;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.File;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.example.shelldemo.validate.DatabaserOperationValidator;
import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.spi.DatabaseVendor;

public class StatementExecutor {
    private static final Logger logger = LogManager.getLogger(StatementExecutor.class);
    private final Connection connection;
    private final DatabaserOperationValidator validator;
    
    public StatementExecutor(Connection connection, DatabaserOperationValidator validator) {
        this.connection = connection;
        this.validator = validator;
        logger.debug("StatementExecutor initialized");
    }
    
    public boolean isPLSQL(String sql) {
        return validator.isPLSQL(sql);
    }
    
    public void executeStatement(String sql, StatementHandler handler) throws SQLException {
        logger.debug("Executing SQL statement: {}", sql);
        if (validator.isPLSQL(sql)) {
            logger.debug("Detected PL/SQL statement, using PL/SQL execution path");
            executePLSQL(sql, handler);
        } else {
            logger.debug("Using regular SQL execution path");
            executeRegular(sql, handler);
        }
    }
    
    private void executePLSQL(String sql, StatementHandler handler) throws SQLException {
        logger.debug("Creating statement for PL/SQL execution");
        try (Statement stmt = connection.createStatement()) {
            handler.handle(stmt, sql);
            logger.debug("PL/SQL statement executed successfully");
        } catch (SQLException e) {
            logger.error("Failed to execute PL/SQL statement: {}", sql, e);
            throw e;
        }
    }
    
    private void executeRegular(String sql, StatementHandler handler) throws SQLException {
        logger.debug("Creating prepared statement for regular SQL execution");
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            handler.handle(stmt, sql);
            logger.debug("Regular SQL statement executed successfully");
        } catch (SQLException e) {
            // Extract Oracle error code and message
            String errorCode = e.getSQLState() != null ? e.getSQLState() : String.valueOf(e.getErrorCode());
            String errorMessage = e.getMessage();
            
            // Format a more user-friendly error message
            String formattedMessage = String.format(
                "Failed to execute SQL statement%nError [%s]: %s%nStatement: %s",
                errorCode,
                errorMessage,
                sql
            );
            
            logger.error(formattedMessage);
            throw new DatabaseException(formattedMessage, e, DatabaseException.ErrorType.OP_QUERY);
        }
    }
    
    public String getExplainPlan(String sql) throws SQLException {
        logger.debug("Getting explain plan for SQL: {}", sql);
        String explainSql = "EXPLAIN PLAN FOR " + sql;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(explainSql);
            try (ResultSet rs = stmt.executeQuery("SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())")) {
                StringBuilder plan = new StringBuilder();
                while (rs.next()) {
                    plan.append(rs.getString(1)).append("\n");
                }
                return plan.toString();
            }
        }
    }

    public void validateScript(String scriptPath, boolean showExplainPlan, DatabaseVendor vendor) throws SQLException {
        logger.debug("Validating script: {}", scriptPath);
        Map<Integer, String> statements = SqlScriptParser.parseSqlFile(new File(scriptPath), vendor);
        
        for (Map.Entry<Integer, String> entry : statements.entrySet()) {
            String sql = entry.getValue();
            logger.debug("Validating statement #{}: {}", entry.getKey(), sql);
            
            if (showExplainPlan) {
                String plan = getExplainPlan(sql);
                logger.info("Explain plan for statement #{}: \n{}", entry.getKey(), plan);
            }
            
            // Try to prepare the statement to validate syntax
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                logger.debug("Statement #{} validated successfully", entry.getKey());
            }
        }
        logger.info("Script validation completed successfully");
    }
}