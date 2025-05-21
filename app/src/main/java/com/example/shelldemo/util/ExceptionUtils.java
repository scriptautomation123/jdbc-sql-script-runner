package com.example.shelldemo.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.sql.SQLException;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.DatabaseOperationException;

/**
 * Provides standardized exception handling for database operations.
 * Centralizes error handling logic to ensure consistent error messages and logging.
 */
public final class ExceptionUtils {
    
    private ExceptionUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Handles a SQLException by logging it and creating an appropriate DatabaseException.
     *
     * @param e The SQLException to handle
     * @param operation A description of the operation that failed
     * @param type The error type
     * @param logger The logger to use
     * @return A new DatabaseException
     */
    public static DatabaseException handleSQLException(
            SQLException e, String operation, ErrorType type, Logger logger) {
        String message = String.format("Failed to %s: %s", operation, e.getMessage());
        ThreadContext.put("operation", operation);
        ThreadContext.put("errorType", type.name());
        logger.error(message, e);
        ThreadContext.clearAll();
        return new DatabaseException(message, e, type);
    }
    
    /**
     * Logs an exception and rethrows it as a DatabaseException.
     * This method is useful for catch blocks to standardize exception handling.
     *
     * @param e The exception to handle
     * @param operation A description of the operation that failed
     * @param logger The logger to use
     * @throws DatabaseOperationException The wrapped exception
     */
    public static void logAndRethrow(Exception e, String operation, Logger logger) {
        if (e instanceof SQLException sqlEx) {
            throw handleSQLException(sqlEx, operation, getErrorTypeForSQLException(sqlEx), logger);
        } else {
            ThreadContext.put("operation", operation);
            logger.error("Error during {}: {}", operation, e.getMessage(), e);
            ThreadContext.clearAll();
            throw new DatabaseOperationException(
                "Failed to " + operation + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines the most appropriate ErrorType for a SQLException.
     * This method analyzes the exception to categorize it correctly.
     *
     * @param e The SQLException to analyze
     * @return The appropriate ErrorType
     */
    private static ErrorType getErrorTypeForSQLException(SQLException e) {
        // SQL State patterns
        String sqlState = e.getSQLState();
        if (sqlState == null) {
            return ErrorType.OP_QUERY;
        }
        
        // Categorize by SQL state
        if (sqlState.startsWith("08")) {
            return ErrorType.CONN_FAILED; // Connection errors
        } else if (sqlState.startsWith("42")) {
            return ErrorType.SYNTAX_ERROR; // Syntax errors
        } else if (sqlState.startsWith("23")) {
            return ErrorType.CONSTRAINT_VIOLATION; // Constraint violations
        } else if (sqlState.startsWith("22")) {
            return ErrorType.DATA_ERROR; // Data errors
        } else {
            return ErrorType.OP_QUERY; // Default
        }
    }
}
