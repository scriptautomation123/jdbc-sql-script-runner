package com.example.shelldemo.validate;


import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.File;
import java.sql.Statement;

public class DatabaserOperationValidator {
    private static final Logger logger = LogManager.getLogger(DatabaserOperationValidator.class);
    
    private final String dbType;

    public DatabaserOperationValidator(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Main validation method that orchestrates the validation process
     */
    public void validateScript(Connection conn, String scriptPath, boolean showExplainPlan, com.example.shelldemo.spi.DatabaseVendor vendor) throws SQLException {
        logger.info("Starting validation of script: {}", scriptPath);
        try {
            java.util.Map<Integer, String> parsedStatements = SqlScriptParser.parseSqlFile(new File(scriptPath), vendor);
            java.util.List<String> statements = new java.util.ArrayList<>(parsedStatements.values());
            
            DatabaseOperationValidationContext context = new DatabaseOperationValidationContext(conn, showExplainPlan);
            for (String statement : statements) {
                validateStatement(statement.trim(), context);
            }
            
            logger.info("Validation completed successfully. {} statements validated.", 
                context.getStatementCount());
        } catch (SQLException e) {
            logger.error("SQL error during validation: {}", e.getMessage(), e);
            throw new ValidationException(
                "SQL error during validation of script: " + scriptPath,
                e,
                scriptPath
            );
        } catch (Exception e) {
            logger.error("Failed to validate script: {}", scriptPath, e);
            throw new ValidationException(
                "Failed to validate SQL script: " + scriptPath,
                e,
                scriptPath
            );
        }
    }

    /**
     * Validates a single statement
     */
    private void validateStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        if (statement.isEmpty() || statement.startsWith("--")) {
            return;
        }

        int statementNum = context.incrementAndGetCount();
        logger.info("Validating statement {}", statementNum);

        try {
            if (isPLSQL(statement)) {
                validatePLSQLStatement(statement, context);
            } else {
                validateSQLStatement(statement, context);
            }
            logger.info("Statement {} is valid", statementNum);
        } catch (SQLException e) {
            String error = formatValidationError(e, statementNum);
            throw new DatabaseException(
                error,
                e,
                ErrorType.PARSE_SQL
            );
        }
    }

    /**
     * Validates PL/SQL specific statements
     */
    private void validatePLSQLStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        logger.debug("Validating PL/SQL statement");
        try {
            validateSyntax(context.getConnection(), statement, true);
        } catch (SQLException e) {
            throw new DatabaseException(
                "Invalid PL/SQL syntax",
                e,
                ErrorType.PARSE_SQL
            );
        }
    }

    /**
     * Validates regular SQL statements
     */
    private void validateSQLStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        try {
            if (context.isShowExplainPlan()) {
                String plan = getExplainPlan(context.getConnection(), statement);
                logger.info("Execution plan:\n{}", plan);
            } else {
                validateSyntax(context.getConnection(), statement, false);
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                "Invalid SQL syntax",
                e,
                ErrorType.PARSE_SQL
            );
        }
    }

    /**
     * Validates SQL or PL/SQL syntax
     */
    private void validateSyntax(Connection conn, String sql, boolean isPLSQL) throws SQLException {
        String validateQuery = switch (dbType.toLowerCase()) {
            case "oracle" -> isPLSQL ? 
                "BEGIN DBMS_SQL.PARSE(:1, :2, DBMS_SQL.NATIVE); END;" :
                "EXPLAIN PLAN FOR " + sql;
            case "postgresql" -> "EXPLAIN " + sql;
            case "mysql" -> "EXPLAIN FORMAT=TREE " + sql;
            case "sqlserver" -> "SET PARSEONLY ON; " + sql + "; SET PARSEONLY OFF;";
            default -> throw new SQLException("Syntax validation not supported for " + dbType);
        };

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(validateQuery);
        }
    }

    /**
     * Formats validation error message
     */
    private String formatValidationError(SQLException e, int statementNum) {
        return String.format("Validation failed for statement %d: %s", 
            statementNum, 
            e.getMessage());
    }

    /**
     * Determines if a statement is PL/SQL
     */
    public boolean isPLSQL(String statement) {
        String normalized = statement.trim().toLowerCase();
        return normalized.startsWith("begin") || 
               normalized.startsWith("declare") || 
               normalized.startsWith("create or replace");
    }

    /**
     * Gets the explain plan for a SQL statement
     */
    public String getExplainPlan(Connection conn, String sql) throws SQLException {
        String explainQuery = switch (dbType.toLowerCase()) {
            case "oracle" -> "EXPLAIN PLAN FOR " + sql;
            case "postgresql" -> "EXPLAIN (ANALYZE false, COSTS true, FORMAT TEXT) " + sql;
            case "mysql" -> "EXPLAIN FORMAT=TREE " + sql;
            case "sqlserver" -> "SET SHOWPLAN_XML ON; " + sql + "; SET SHOWPLAN_XML OFF;";
            default -> throw new SQLException("Explain plan not supported for " + dbType);
        };
        
        try (Statement stmt = conn.createStatement()) {
            StringBuilder plan = new StringBuilder();
            try (var rs = stmt.executeQuery(explainQuery)) {
                while (rs.next()) {
                    plan.append(rs.getString(1)).append("\n");
                }
            }
            return plan.toString();
        } catch (SQLException e) {
            throw new DatabaseException(
                "Failed to get explain plan",
                e,
                ErrorType.OP_QUERY
            );
        }
    }
}