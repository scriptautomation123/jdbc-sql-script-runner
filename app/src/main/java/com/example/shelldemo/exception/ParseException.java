package com.example.shelldemo.exception;

/**
 * Exception thrown when an error occurs during parsing SQL or related content.
 */
public class ParseException extends DatabaseException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new parse exception with a message.
     * 
     * @param message The error message
     */
    public ParseException(String message) {
        super(message, ErrorType.PARSE_SQL);
    }

    /**
     * Creates a new parse exception with a message and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause, ErrorType.PARSE_SQL);
    }

    /**
     * Creates a new parse exception with a message and file path.
     * 
     * @param message The error message
     * @param filePath The path of the file being parsed
     */
    public ParseException(String message, String filePath) {
        super(String.format("%s in file: %s", message, filePath), ErrorType.PARSE_SQL);
    }

    /**
     * Creates a new parse exception with a message, cause, and file path.
     * 
     * @param message The error message
     * @param cause The underlying cause
     * @param filePath The path of the file being parsed
     */
    public ParseException(String message, Throwable cause, String filePath) {
        super(String.format("%s in file: %s", message, filePath), cause, ErrorType.PARSE_SQL);
    }
}
