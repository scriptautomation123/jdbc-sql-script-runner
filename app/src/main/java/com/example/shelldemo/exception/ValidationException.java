package com.example.shelldemo.exception;

/**
 * Exception thrown when validation of SQL statements, scripts or operations fails.
 */
public class ValidationException extends DatabaseException {
    
    private final String scriptPath;
    
    public ValidationException(String message) {
        super(message, ErrorType.PARSE_SQL);
        this.scriptPath = null;
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause, ErrorType.PARSE_SQL);
        this.scriptPath = null;
    }
    
    public ValidationException(String message, String scriptPath) {
        super(message, ErrorType.PARSE_SQL, null, "Script: " + scriptPath);
        this.scriptPath = scriptPath;
    }
    
    public ValidationException(String message, Throwable cause, String scriptPath) {
        super(message, cause, ErrorType.PARSE_SQL, null, "Script: " + scriptPath);
        this.scriptPath = scriptPath;
    }
    
    /**
     * Gets the path to the script that was being validated.
     *
     * @return the script path or null if not applicable
     */
    public String getScriptPath() {
        return scriptPath;
    }
}
