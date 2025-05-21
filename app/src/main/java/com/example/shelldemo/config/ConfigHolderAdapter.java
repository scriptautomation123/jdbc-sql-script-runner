package com.example.shelldemo.config;

import java.util.Map;

/**
 * Implementation of ConfigurationService that adapts the existing ConfigurationHolder.
 * This allows for a clean transition to the new architecture without changing the underlying
 * configuration mechanism.
 */
public class ConfigHolderAdapter implements ConfigurationService {
    private final ConfigurationHolder configHolder;
    
    /**
     * Creates a new ConfigHolderAdapter.
     */
    public ConfigHolderAdapter() {
        this.configHolder = ConfigurationHolder.getInstance();
    }
    
    /**
     * Creates a new ConfigHolderAdapter with a specific ConfigurationHolder.
     * This constructor is primarily used for testing.
     *
     * @param configHolder The ConfigurationHolder to use
     */
    public ConfigHolderAdapter(ConfigurationHolder configHolder) {
        this.configHolder = configHolder;
    }
    
    @Override
    public Map<String, Object> getDatabaseConfig(String dbType) {
        return configHolder.getDatabaseConfig(dbType);
    }
    
    @Override
    public String getSqlTemplate(String dbType, String templateName) {
        return configHolder.getSqlTemplate(dbType, templateName);
    }
    
    @Override
    public int getDefaultPort(String dbType) {
        return configHolder.getDefaultPort(dbType);
    }
    
    @Override
    public String getJdbcTemplate(String dbType, String templateName) {
        return configHolder.getJdbcClientTemplate(dbType, templateName);
    }
    
    @Override
    public Map<String, String> getDatabaseProperties(String dbType) {
        return configHolder.getDatabaseProperties(dbType);
    }
    
    @Override
    public void setRuntimeProperty(String key, String value) {
        configHolder.setRuntimeProperty(key, value);
    }
    
    @Override
    public String getRuntimeProperty(String key) {
        return configHolder.getRuntimeProperty(key);
    }
    
    @Override
    public boolean isValidDbType(String dbType) {
        return configHolder.isValidDbType(dbType);
    }
}
