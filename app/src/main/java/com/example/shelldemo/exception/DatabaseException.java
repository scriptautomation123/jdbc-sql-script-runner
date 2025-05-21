package com.example.shelldemo.exception;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.shelldemo.config.ConfigurationHolder;

/**
 * Base exception class for all database-related exceptions.
 * Provides common logging and context handling functionality.
 */
public class DatabaseException extends RuntimeException {
    private static final Logger logger = LogManager.getLogger(DatabaseException.class);

    public enum ErrorType {
        // Configuration errors
        CONFIG_NOT_FOUND("CFG_001", "Configuration file not found"),
        CONFIG_INVALID("CFG_002", "Invalid configuration"),
        
        // Connection errors
        CONN_FAILED("CONN_001", "Connection failed"),
        CONN_TIMEOUT("CONN_002", "Connection timeout"),
        CONN_AUTH("CONN_003", "Authentication failed"),
        
        // Operation errors
        OP_TRANSACTION("OP_001", "Transaction failed"),
        OP_QUERY("OP_002", "Query execution failed"),
        OP_PROCEDURE("OP_003", "Stored procedure execution failed"),
        OP_BATCH("OP_004", "Batch processing failed"),
        OP_UPDATE("OP_005", "Update operation failed"),
        OP_DDL("OP_006", "DDL operation failed"),
        
        // Data errors
        SYNTAX_ERROR("DATA_001", "SQL syntax error"),
        CONSTRAINT_VIOLATION("DATA_002", "Constraint violation"),
        DATA_ERROR("DATA_003", "Data error"),
        
        // Parser errors
        PARSE_SQL("PARSE_001", "SQL parsing failed"),
        PARSE_PROCEDURE("PARSE_002", "Stored procedure parsing failed"),
        
        // Oracle specific
        ORACLE_TNS("ORA_001", "TNS connection error"),
        ORACLE_INVALID_USER("ORA_002", "Invalid Oracle user"),
        
        // Generic
        UNKNOWN("ERR_999", "Unknown error");

        private final String code;
        private final String defaultMessage;

        ErrorType(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() { return code; }
        public String getDefaultMessage() { return defaultMessage; }
    }

    private final ErrorType errorType;
    private final String dbmsErrorCode;
    private final String context;

    public DatabaseException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.dbmsErrorCode = null;
        this.context = null;
    }

    public DatabaseException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.dbmsErrorCode = null;
        this.context = null;
    }

    public DatabaseException(String message, ErrorType errorType, String dbmsErrorCode, String context) {
        super(message);
        this.errorType = errorType;
        this.dbmsErrorCode = dbmsErrorCode;
        this.context = context;
    }

    public ErrorType getErrorType() { return errorType; }
    public String getDbmsErrorCode() { return dbmsErrorCode; }
    public String getContext() { return context; }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(" [").append(errorType.getCode()).append("]");
        if (dbmsErrorCode != null) {
            message.append(" (DBMS Error: ").append(dbmsErrorCode).append(")");
        }
        if (context != null) {
            message.append(" Context: ").append(context);
        }
        return message.toString();
    }

    /**
     * Creates a DatabaseException from an SQLException using database-specific error mappings.
     * 
     * @param message The error message
     * @param e The SQLException
     * @param dbType The database type
     * @param context Additional context information
     * @return A DatabaseException with the appropriate error type
     */
    public static DatabaseException fromSQLException(String message, SQLException e, String dbType, String context) {
        ErrorType errorType = determineErrorType(e, dbType);
        String dbmsErrorCode = extractErrorCode(e, dbType);
        return new DatabaseException(message, e, errorType, dbmsErrorCode, context);
    }

    /**
     * Constructor that includes all details including the original exception
     */
    public DatabaseException(String message, Throwable cause, ErrorType errorType, String dbmsErrorCode, String context) {
        super(message, cause);
        this.errorType = errorType;
        this.dbmsErrorCode = dbmsErrorCode;
        this.context = context;
    }

    /**
     * Determines the error type from an SQLException using Java 21 pattern matching.
     * Uses database-specific error mappings to translate error codes to application error types.
     */
    private static ErrorType determineErrorType(SQLException e, String dbType) {
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(dbType);
        
        // Use pattern matching and switch expression
        return switch (dbmsConfig.get("error-mappings")) {
            case Map<?,?> errorMappings -> {
                String errorCode = extractErrorCode(e, dbType);
                
                if (errorCode != null && errorMappings.containsKey(errorCode)) {
                    yield switch (errorMappings.get(errorCode)) {
                        case String errorType -> {
                            try {
                                yield ErrorType.valueOf(errorType);
                            } catch (IllegalArgumentException ex) {
                                logger.warn("Invalid error type mapping: {}", errorType);
                                yield ErrorType.UNKNOWN;
                            }
                        }
                        case null, default -> ErrorType.UNKNOWN;
                    };
                } else {
                    yield mapGenericError(e);
                }
            }
            case null, default -> mapGenericError(e);
        };
    }
    
    /**
     * Maps generic SQL errors to application error types using pattern matching.
     */
    private static ErrorType mapGenericError(SQLException e) {
        // Removed unused variable errorCode
        String sqlState = e.getSQLState();
        
        // Pattern match on SQL state categories
        if (sqlState != null) {
            return switch (sqlState.substring(0, 2)) {
                case "08" -> ErrorType.CONN_FAILED;   // Connection errors
                case "28" -> ErrorType.CONN_AUTH;     // Authorization errors
                case "22" -> ErrorType.OP_QUERY;      // Data exceptions
                case "23" -> ErrorType.OP_QUERY;      // Constraint violations
                case "42" -> ErrorType.PARSE_SQL;     // Syntax errors
                case "25" -> ErrorType.OP_TRANSACTION; // Transaction errors
                default -> ErrorType.UNKNOWN;
            };
        }
        
        return ErrorType.UNKNOWN;
    }

    /**
     * Extract database-specific error code using regex pattern matching.
     * Uses text block for multiline pattern definitions.
     */
    private static String extractErrorCode(SQLException e, String dbType) {
        if (e.getMessage() == null) return null;
        
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(dbType);
        
        // Use pattern matching with instanceof
        return switch (dbmsConfig.get("error-code-pattern")) {
            case String errorPattern -> {
                Pattern pattern = Pattern.compile(errorPattern);
                Matcher matcher = pattern.matcher(e.getMessage());
                yield matcher.find() ? matcher.group(1) : null;
            }
            case null, default -> null;
        };
    }
} 