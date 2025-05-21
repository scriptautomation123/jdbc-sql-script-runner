package com.example.shelldemo.config;

import java.util.Map;

/**
 * Service interface for accessing configuration settings.
 * Abstracts the configuration source and provides a clean API for retrieving settings.
 */
public interface ConfigurationService {
    
    /**
     * Retrieves the configuration for a specific database type.
     *
     * @param dbType The database type
     * @return A map of configuration properties
     */
    Map<String, Object> getDatabaseConfig(String dbType);
    
    /**
     * Retrieves a SQL template for a specific database type.
     *
     * @param dbType The database type
     * @param templateName The template name
     * @return The SQL template
     */
    String getSqlTemplate(String dbType, String templateName);
    
    /**
     * Retrieves the default port for a specific database type.
     *
     * @param dbType The database type
     * @return The default port
     */
    int getDefaultPort(String dbType);
    
    /**
     * Retrieves a JDBC template for a specific database type.
     *
     * @param dbType The database type
     * @param templateName The template name
     * @return The JDBC template
     */
    String getJdbcTemplate(String dbType, String templateName);
    
    /**
     * Retrieves the database properties for a specific database type.
     *
     * @param dbType The database type
     * @return A map of database properties
     */
    Map<String, String> getDatabaseProperties(String dbType);
    
    /**
     * Sets a runtime property.
     *
     * @param key The property key
     * @param value The property value
     */
    void setRuntimeProperty(String key, String value);
    
    /**
     * Retrieves a runtime property.
     *
     * @param key The property key
     * @return The property value
     */
    String getRuntimeProperty(String key);
    
    /**
     * Checks if a database type is valid.
     *
     * @param dbType The database type to check
     * @return true if the database type is valid
     */
    boolean isValidDbType(String dbType);
}
