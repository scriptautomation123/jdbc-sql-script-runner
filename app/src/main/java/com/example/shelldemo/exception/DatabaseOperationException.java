package com.example.shelldemo.exception;

/**
 * Exception thrown when a database operation fails.
 * This can be due to configuration issues, connection problems,
 * or errors during script execution.
 */
public class DatabaseOperationException extends DatabaseException {
    
    public DatabaseOperationException(String message) {
        super(message, ErrorType.OP_QUERY);
    }
    
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause, ErrorType.OP_QUERY);
    }
    
    public DatabaseOperationException(String message, ErrorType errorType) {
        super(message, errorType);
    }
    
    public DatabaseOperationException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause, errorType);
    }
    
    public DatabaseOperationException(String message, ErrorType errorType, String dbmsErrorCode, String context) {
        super(message, errorType, dbmsErrorCode, context);
    }
}
