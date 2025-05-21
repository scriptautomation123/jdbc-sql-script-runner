package com.example.shelldemo.exception;

/**
 * Exception thrown when errors occur during the parsing of stored procedure parameters or definitions.
 */
public class ProcedureParseException extends DatabaseException {
    
    private final String procedureName;
    private final String parameterName;
    
    public ProcedureParseException(String message) {
        super(message, ErrorType.PARSE_PROCEDURE);
        this.procedureName = null;
        this.parameterName = null;
    }
    
    public ProcedureParseException(String message, Throwable cause) {
        super(message, cause, ErrorType.PARSE_PROCEDURE);
        this.procedureName = null;
        this.parameterName = null;
    }
    
    public ProcedureParseException(String message, String procedureName) {
        super(message, ErrorType.PARSE_PROCEDURE, null, "Procedure: " + procedureName);
        this.procedureName = procedureName;
        this.parameterName = null;
    }
    
    public ProcedureParseException(String message, String procedureName, String parameterName) {
        super(message, ErrorType.PARSE_PROCEDURE, null, 
            "Procedure: " + procedureName + ", Parameter: " + parameterName);
        this.procedureName = procedureName;
        this.parameterName = parameterName;
    }
    
    public ProcedureParseException(String message, Throwable cause, String procedureName) {
        super(message, cause, ErrorType.PARSE_PROCEDURE, null, "Procedure: " + procedureName);
        this.procedureName = procedureName;
        this.parameterName = null;
    }
    
    /**
     * Gets the name of the procedure being parsed when the exception occurred.
     *
     * @return the procedure name or null if not applicable
     */
    public String getProcedureName() {
        return procedureName;
    }
    
    /**
     * Gets the name of the parameter being parsed when the exception occurred.
     *
     * @return the parameter name or null if not applicable
     */
    public String getParameterName() {
        return parameterName;
    }
}
