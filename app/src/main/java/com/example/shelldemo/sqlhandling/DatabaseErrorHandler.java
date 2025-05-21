package com.example.shelldemo.sqlhandling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.sql.SQLException;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.DatabaseErrorFormatter;

public class DatabaseErrorHandler {
    private static final Logger logger = LogManager.getLogger(DatabaseErrorHandler.class);
    private final DatabaseErrorFormatter formatter;
    
    public DatabaseErrorHandler(String dbType) {
        this.formatter = new DatabaseErrorFormatter(dbType);
    }
    
    public DatabaseException handleSQLException(SQLException e, String operation) {
        logger.error("Error during {}: {}", operation, e.getMessage());
        return formatter.format(e);
    }
    
    public DatabaseException handleException(Exception e, String operation) {
        logger.error("Error during {}: {}", operation, e.getMessage());
        if (e instanceof SQLException sqlException) {
            return formatter.format(sqlException);
        }
        return new DatabaseException("Operation failed: " + operation, e, ErrorType.OP_QUERY);
    }
}