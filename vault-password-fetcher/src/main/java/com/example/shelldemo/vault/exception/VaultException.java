package com.example.shelldemo.vault.exception;

/**
 * Exception thrown when operations involving Vault secret management fail.
 * This covers authentication, secret retrieval, and response parsing issues.
 */
public class VaultException extends RuntimeException {
    
    private final String vaultUrl;
    private final String secretPath;
    
    public VaultException(String message) {
        super(message);
        this.vaultUrl = null;
        this.secretPath = null;
    }
    
    public VaultException(String message, Throwable cause) {
        super(message, cause);
        this.vaultUrl = null;
        this.secretPath = null;
    }
    
    public VaultException(String message, String vaultUrl, String secretPath) {
        super(message + (vaultUrl != null ? " (URL: " + vaultUrl + ")" : "") + 
              (secretPath != null ? " (Secret path: " + secretPath + ")" : ""));
        this.vaultUrl = vaultUrl;
        this.secretPath = secretPath;
    }
    
    public VaultException(String message, Throwable cause, String vaultUrl, String secretPath) {
        super(message + (vaultUrl != null ? " (URL: " + vaultUrl + ")" : "") + 
              (secretPath != null ? " (Secret path: " + secretPath + ")" : ""), cause);
        this.vaultUrl = vaultUrl;
        this.secretPath = secretPath;
    }
    
    /**
     * Gets the Vault server URL that was being accessed.
     *
     * @return the Vault URL or null if not applicable
     */
    public String getVaultUrl() {
        return vaultUrl;
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
