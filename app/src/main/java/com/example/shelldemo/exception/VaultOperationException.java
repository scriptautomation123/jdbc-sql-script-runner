package com.example.shelldemo.exception;

/**
 * Exception thrown when operations involving Vault secret management fail.
 * This covers authentication, secret retrieval, and response parsing issues.
 */
public class VaultOperationException extends DatabaseException {
    
    private final String secretPath;
    
    public VaultOperationException(String message) {
        super(message, ErrorType.CONFIG_NOT_FOUND);
        this.secretPath = null;
    }
    
    public VaultOperationException(String message, Throwable cause) {
        super(message, cause, ErrorType.CONFIG_NOT_FOUND);
        this.secretPath = null;
    }
    
    public VaultOperationException(String message, String secretPath) {
        super(message, ErrorType.CONFIG_NOT_FOUND, null, "Secret path: " + secretPath);
        this.secretPath = secretPath;
    }
    
    public VaultOperationException(String message, Throwable cause, String secretPath) {
        super(message, cause, ErrorType.CONFIG_NOT_FOUND, null, "Secret path: " + secretPath);
        this.secretPath = secretPath;
    }
    
    /**
     * Gets the Vault secret path that was being accessed.
     *
     * @return the secret path or null if not applicable
     */
    public String getSecretPath() {
        return secretPath;
    }
}
