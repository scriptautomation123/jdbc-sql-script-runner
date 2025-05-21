package com.example.shelldemo.parser;

import java.sql.Connection;
import java.sql.SQLException;

import com.example.shelldemo.spi.DatabaseVendor;

/**
 * Represents different types of SQL statements with a sealed hierarchy.
 * This allows exhaustive pattern matching in Java 21 switch expressions.
 */
public sealed interface SqlStatement 
    permits SqlStatement.QueryStatement, 
            SqlStatement.DmlStatement, 
            SqlStatement.DdlStatement, 
            SqlStatement.ProcedureStatement {
    
    /**
     * Returns the SQL statement text.
     *
     * @return The SQL text
     */
    String getText();
    
    /**
     * Determines if this statement type requires a ResultSet.
     *
     * @return true if the statement returns a ResultSet
     */
    boolean requiresResultSet();
    
    /**
     * Validates the statement against a database vendor and connection.
     * Default implementation performs basic validation.
     *
     * @param vendor The database vendor
     * @param connection The database connection
     * @throws SQLException if validation fails
     */
    default void validate(DatabaseVendor vendor, Connection connection) throws SQLException {
        // Basic validation by default
        if (getText() == null || getText().isBlank()) {
            throw new SQLException("SQL statement cannot be null or blank");
        }
    }
    
    /**
     * Represents a SQL query statement (SELECT).
     */
    record QueryStatement(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
        
        @Override
        public boolean requiresResultSet() {
            return true;
        }
    }
    
    /**
     * Represents a DML statement (INSERT, UPDATE, DELETE, MERGE).
     */
    record DmlStatement(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
        
        @Override
        public boolean requiresResultSet() {
            return false;
        }
    }
    
    /**
     * Represents a DDL statement (CREATE, ALTER, DROP).
     */
    record DdlStatement(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
        
        @Override
        public boolean requiresResultSet() {
            return false;
        }
    }
    
    /**
     * Represents a stored procedure or PL/SQL block.
     */
    record ProcedureStatement(String text) implements SqlStatement {
        @Override
        public String getText() {
            return text;
        }
        
        @Override
        public boolean requiresResultSet() {
            // Procedures may or may not return results
            // This would need to be determined at runtime
            return false;
        }
    }
}
