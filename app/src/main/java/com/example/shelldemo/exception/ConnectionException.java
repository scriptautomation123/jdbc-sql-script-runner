package com.example.shelldemo.exception;

import java.sql.SQLException;

/**
 * Exception thrown when there are issues establishing or maintaining database connections.
 */
public class ConnectionException extends DatabaseException {
    private static final long serialVersionUID = 1L;
    
    private final String hostInfo;
    private final String vendorName;
    
    /**
     * Creates a new connection exception with a message.
     *
     * @param message The error message
     */
    public ConnectionException(String message) {
        super(message, ErrorType.CONN_FAILED);
        this.hostInfo = null;
        this.vendorName = null;
    }
    
    /**
     * Creates a new connection exception with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause, ErrorType.CONN_FAILED);
        this.hostInfo = null;
        this.vendorName = null;
    }
    
    /**
     * Creates a new connection exception with details about the connection.
     *
     * @param message The error message
     * @param cause The underlying cause
     * @param vendorName The database vendor name
     * @param hostInfo Host connection details
     */
    public ConnectionException(String message, Throwable cause, String vendorName, String hostInfo) {
        super(message, cause, ErrorType.CONN_FAILED);
        this.hostInfo = hostInfo;
        this.vendorName = vendorName;
    }
    
    /**
     * Factory method to create a ConnectionException from an SQLException
     *
     * @param message The error message
     * @param sqlException The SQLException that caused this exception
     * @param vendorName The database vendor name
     * @param hostInfo Host connection details
     * @return A new ConnectionException with details from the SQLException
     */
    public static ConnectionException fromSQLException(String message, SQLException sqlException, 
                                                     String vendorName, String hostInfo) {
        ConnectionException ex = new ConnectionException(
            String.format("%s: %s (SQL State: %s, Vendor Code: %d)", 
                message, 
                sqlException.getMessage(),
                sqlException.getSQLState(),
                sqlException.getErrorCode()
            ),
            sqlException,
            vendorName,
            hostInfo
        );
        return ex;
    }
    
    /**
     * Gets the host information.
     *
     * @return The host information or null if not available
     */
    public String getHostInfo() {
        return hostInfo;
    }
    
    /**
     * Gets the database vendor name.
     *
     * @return The vendor name or null if not available
     */
    public String getVendorName() {
        return vendorName;
    }
}
