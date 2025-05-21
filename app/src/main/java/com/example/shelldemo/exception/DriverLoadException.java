package com.example.shelldemo.exception;

/**
 * Exception thrown when there are issues loading a JDBC driver.
 */
public class DriverLoadException extends DatabaseException {
    private static final long serialVersionUID = 1L;
    
    private final String driverPath;
    private final String driverClass;
    
    /**
     * Creates a new driver load exception with a message.
     *
     * @param message The error message
     */
    public DriverLoadException(String message) {
        super(message, ErrorType.CONN_FAILED);
        this.driverPath = null;
        this.driverClass = null;
    }
    
    /**
     * Creates a new driver load exception with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public DriverLoadException(String message, Throwable cause) {
        super(message, cause, ErrorType.CONN_FAILED);
        this.driverPath = null;
        this.driverClass = null;
    }
    
    /**
     * Creates a new driver load exception with driver path information.
     *
     * @param message The error message
     * @param cause The underlying cause
     * @param driverPath Path to the driver that failed to load
     */
    public DriverLoadException(String message, Throwable cause, String driverPath) {
        super(message, cause, ErrorType.CONN_FAILED);
        this.driverPath = driverPath;
        this.driverClass = null;
    }
    
    /**
     * Creates a new driver load exception with driver class information.
     *
     * @param message The error message
     * @param cause The underlying cause
     * @param driverClass Class name of the driver that failed to load
     * @param isClassName Flag to indicate the string is a class name
     */
    public DriverLoadException(String message, Throwable cause, String driverClass, boolean isClassName) {
        super(message, cause, ErrorType.CONN_FAILED);
        this.driverPath = null;
        this.driverClass = isClassName ? driverClass : null;
    }
    
    /**
     * Gets the driver path if available.
     *
     * @return The driver path or null if not available
     */
    public String getDriverPath() {
        return driverPath;
    }
    
    /**
     * Gets the driver class name if available.
     *
     * @return The driver class name or null if not available
     */
    public String getDriverClass() {
        return driverClass;
    }
}