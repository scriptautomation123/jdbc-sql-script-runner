package com.example.shelldemo.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import com.example.shelldemo.config.ConfigurationService;

/**
 * Enhanced sealed interface for database vendor implementations.
 * Provides vendor-specific behavior for various database operations.
 */
public sealed interface DatabaseVendor
    permits OracleVendor, PostgreSqlVendor, MySqlVendor, SqlServerVendor, DatabaseVendorAdapter {

    /**
     * Gets the canonical name of this database vendor.
     *
     * @return The vendor name
     */
    String getVendorName();
    
    /**
     * Builds a connection URL for this database type.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param database Database/service name 
     * @param connectionType Optional connection type (e.g., "thin" for Oracle)
     * @return JDBC connection URL
     */
    String buildConnectionUrl(String host, int port, String database, String connectionType);
    
    /**
     * Gets default connection properties for this vendor.
     *
     * @return Default connection properties
     */
    Properties getDefaultConnectionProperties();
    
    /**
     * Executes vendor-specific initialization on a new connection.
     * 
     * @param connection The JDBC connection to initialize
     */
    default void initializeConnection(Connection connection) {
        // Default implementation does nothing
    }
    
    /**
     * Determines if a SQL statement is a PL/SQL block.
     *
     * @param sql The SQL statement
     * @return true if the statement is a PL/SQL block
     */
    boolean isPLSQL(String sql);
    
    /**
     * Gets the explain plan SQL for a statement.
     *
     * @param sql The SQL statement
     * @return The explain plan SQL
     */
    default String getExplainPlanSql(String sql) {
        return switch (getVendorName().toLowerCase()) {
            case "oracle" -> "EXPLAIN PLAN FOR " + sql;
            case "postgresql" -> "EXPLAIN (ANALYZE false, COSTS true, FORMAT TEXT) " + sql;
            case "mysql" -> "EXPLAIN " + sql;
            case "sqlserver" -> "SET SHOWPLAN_ALL ON; " + sql + "; SET SHOWPLAN_ALL OFF;";
            default -> throw new UnsupportedOperationException("Explain plan not supported for " + getVendorName());
        };
    }
    
    /**
     * Gets the default port for this vendor.
     *
     * @return The default port
     */
    default int getDefaultPort() {
        return switch (getVendorName().toLowerCase()) {
            case "oracle" -> 1521;
            case "postgresql" -> 5432;
            case "mysql" -> 3306;
            case "sqlserver" -> 1433;
            default -> 0;
        };
    }
    
    /**
     * Gets the JDBC driver class name for this vendor.
     *
     * @return The driver class name
     */
    default String getDriverClassName() {
        return switch (getVendorName().toLowerCase()) {
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "postgresql" -> "org.postgresql.Driver";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default -> throw new UnsupportedOperationException("Unknown driver for " + getVendorName());
        };
    }
    
    /**
     * Gets a validation query for this vendor.
     *
     * @return A simple query that can be used to validate a connection
     */
    default String getValidationQuery() {
        return switch (getVendorName().toLowerCase()) {
            case "oracle" -> "SELECT 1 FROM DUAL";
            case "postgresql", "mysql" -> "SELECT 1";
            case "sqlserver" -> "SELECT 1 as test";
            default -> "SELECT 1";
        };
    }
    
    /**
     * Validates a connection to ensure it's usable.
     *
     * @param connection The connection to validate
     * @return true if the connection is valid
     */
    default boolean validateConnection(Connection connection) {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            
            try (var stmt = connection.createStatement()) {
                try (var rs = stmt.executeQuery(getValidationQuery())) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Creates a procedure call SQL statement.
     *
     * @param procedureName The procedure name
     * @param paramCount The number of parameters
     * @return The procedure call SQL
     */
    default String createProcedureCall(String procedureName, int paramCount) {
        StringBuilder sql = new StringBuilder("{call ")
            .append(procedureName)
            .append("(");
        
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        
        sql.append(")}");
        return sql.toString();
    }
    
    /**
     * Creates a function call SQL statement.
     *
     * @param functionName The function name
     * @param paramCount The number of parameters
     * @return The function call SQL
     */
    default String createFunctionCall(String functionName, int paramCount) {
        StringBuilder sql = new StringBuilder("{? = call ")
            .append(functionName)
            .append("(");
        
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        
        sql.append(")}");
        return sql.toString();
    }

    /**
     * Returns the configuration service used by this vendor.
     */
    ConfigurationService getConfigService();
}
