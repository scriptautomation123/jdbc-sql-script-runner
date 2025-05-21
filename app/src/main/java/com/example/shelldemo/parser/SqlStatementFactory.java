package com.example.shelldemo.parser;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.spi.DatabaseVendor;

/**
 * Factory for creating SQL statement objects.
 * Centralizes SQL statement classification and creation logic.
 */
public class SqlStatementFactory {
    private static final Logger logger = LogManager.getLogger(SqlStatementFactory.class);
    
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DML_PATTERN = Pattern.compile(
        "^\\s*(INSERT|UPDATE|DELETE|MERGE)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DDL_PATTERN = Pattern.compile(
        "^\\s*(CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)\\s+", Pattern.CASE_INSENSITIVE);
    
    private final Map<String, DatabaseVendor> vendorRegistry;
    
    /**
     * Creates a new SqlStatementFactory.
     *
     * @param vendorRegistry Map of database vendors by name
     */
    public SqlStatementFactory(Map<String, DatabaseVendor> vendorRegistry) {
        this.vendorRegistry = vendorRegistry;
    }
    
    /**
     * Creates a SqlStatement object for the given SQL and database type.
     *
     * @param sql The SQL statement text
     * @param dbType The database type
     * @return A SqlStatement object
     */
    public SqlStatement createStatement(String sql, String dbType) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL statement cannot be null or blank");
        }
        
        String normalizedSql = sql.trim();
        DatabaseVendor vendor = vendorRegistry.get(dbType.toLowerCase());
        
        if (vendor == null) {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        
        // First check if it's a PL/SQL block based on vendor-specific logic
        if (isPLSQL(normalizedSql, vendor)) {
            logger.debug("Classified statement as a PL/SQL block");
            return new SqlStatement.ProcedureStatement(normalizedSql);
        }
        
        // Then check other statement types
        if (SELECT_PATTERN.matcher(normalizedSql).find()) {
            logger.debug("Classified statement as a query");
            return new SqlStatement.QueryStatement(normalizedSql);
        } else if (DML_PATTERN.matcher(normalizedSql).find()) {
            logger.debug("Classified statement as a DML statement");
            return new SqlStatement.DmlStatement(normalizedSql);
        } else if (DDL_PATTERN.matcher(normalizedSql).find()) {
            logger.debug("Classified statement as a DDL statement");
            return new SqlStatement.DdlStatement(normalizedSql);
        }
        
        // Default to DML if we can't determine the type
        logger.debug("Could not classify statement, defaulting to DML");
        return new SqlStatement.DmlStatement(normalizedSql);
    }
    
    /**
     * Determines if a SQL statement is a PL/SQL block.
     * Delegates to the vendor-specific implementation.
     *
     * @param sql The SQL statement
     * @param vendor The database vendor
     * @return true if the statement is a PL/SQL block
     */
    private boolean isPLSQL(String sql, DatabaseVendor vendor) {
        try {
            // Use reflection to call isPLSQL method if it exists
            java.lang.reflect.Method method = vendor.getClass().getMethod("isPLSQL", String.class);
            return (boolean) method.invoke(vendor, sql);
        } catch (Exception e) {
            // If the method doesn't exist, use a simple heuristic
            String upperSql = sql.toUpperCase().trim();
            return upperSql.startsWith("BEGIN") || 
                   upperSql.startsWith("DECLARE") ||
                   (upperSql.startsWith("CREATE") && 
                    (upperSql.contains(" FUNCTION") || 
                     upperSql.contains(" PROCEDURE")));
        }
    }
}
