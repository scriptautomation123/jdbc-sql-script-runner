package com.example.shelldemo.exception;

/**
 * Exception thrown when errors occur while loading or processing configuration.
 */
public class ConfigurationException extends DatabaseException {
    
    private final String configKey;
    
    public ConfigurationException(String message) {
        super(message, ErrorType.CONFIG_INVALID);
        this.configKey = null;
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause, ErrorType.CONFIG_INVALID);
        this.configKey = null;
    }
    
    public ConfigurationException(String message, String configKey) {
        super(message, ErrorType.CONFIG_INVALID, null, "Configuration key: " + configKey);
        this.configKey = configKey;
    }
    
    public ConfigurationException(String message, Throwable cause, String configKey) {
        super(message, cause, ErrorType.CONFIG_INVALID, null, "Configuration key: " + configKey);
        this.configKey = configKey;
    }
    
    /**
     * Gets the configuration key that was being accessed when the exception occurred.
     *
     * @return the configuration key or null if not applicable
     */
    public String getConfigKey() {
        return configKey;
    }
}
