package com.example.shelldemo.exception;

import java.io.File;

/**
 * Exception thrown when errors occur during SQL script parsing.
 */
public class SqlParseException extends DatabaseException {
    
    public SqlParseException(String message) {
        super(message, ErrorType.PARSE_SQL);
    }
    
    public SqlParseException(String message, Throwable cause) {
        super(message, cause, ErrorType.PARSE_SQL);
    }
    
    public SqlParseException(String message, String context) {
        super(message, ErrorType.PARSE_SQL, null, context);
    }
    
    public SqlParseException(String message, Throwable cause, String context) {
        super(message, cause, ErrorType.PARSE_SQL, null, context);
    }
    
    /**
     * Validates that a SQL script file exists and is accessible.
     * 
     * @param scriptFile The SQL script file to validate
     * @throws SqlParseException if the file is null, doesn't exist, or isn't a file
     */
    public static void validateScriptFile(File scriptFile) throws SqlParseException {
        if (scriptFile == null) {
            throw new SqlParseException("Script file cannot be null");
        }
        if (!scriptFile.exists()) {
            throw new SqlParseException("Script file does not exist: " + scriptFile.getPath());
        }
        if (!scriptFile.isFile()) {
            throw new SqlParseException("Path is not a file: " + scriptFile.getPath());
        }
    }
}
