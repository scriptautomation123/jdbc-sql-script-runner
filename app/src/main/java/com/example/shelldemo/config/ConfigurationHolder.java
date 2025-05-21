package com.example.shelldemo.config;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton configuration holder that loads and caches application configuration at startup.
 */
public class ConfigurationHolder {
    private static final Logger logger = LogManager.getLogger(ConfigurationHolder.class);
    private static final String CONFIG_PATH = "application.yaml";
    private static ConfigurationHolder instance;
    
    private final Map<String, Object> config;
    private final Map<String, String> runtimeProperties;

    private ConfigurationHolder() {
        this.runtimeProperties = new ConcurrentHashMap<>();
        logger.debug("Initializing ConfigurationHolder and loading configuration");
        this.config = loadConfig();
        logger.info("ConfigurationHolder initialized successfully");
    }

    public static synchronized ConfigurationHolder getInstance() {
        if (instance == null) {
            instance = new ConfigurationHolder();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() {
        logger.debug("Loading configuration from YAML file: {}", CONFIG_PATH);
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream inputStream = null;
            String configPath = System.getProperty("app.config");
            if (configPath != null) {
                logger.debug("Custom config path specified: {}", configPath);
                inputStream = openConfigFile(configPath);
            }
            if (inputStream == null) {
                logger.debug("Trying to load config from class loader resource: {}", CONFIG_PATH);
                inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_PATH);
            if (inputStream == null) {
                logger.debug("Trying to load config from thread context class loader: {}", CONFIG_PATH);
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_PATH);
                }
            }
            if (inputStream == null) {
                String errorMessage = "Configuration file not found: " + CONFIG_PATH;
                logger.error(errorMessage);
                throw new DatabaseException(errorMessage, ErrorType.CONFIG_NOT_FOUND);
            }
            logger.debug("Configuration file loaded successfully");
            return yamlMapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            String errorMessage = "Failed to load configuration";
            logger.error(errorMessage, e);
            throw new DatabaseException(errorMessage, e, ErrorType.CONFIG_NOT_FOUND);
        }
    }

    private InputStream openConfigFile(String configPath) {
        logger.debug("Opening config file at path: {}", configPath);
        try {
            return new java.io.FileInputStream(configPath);
        } catch (java.io.FileNotFoundException e) {
            logger.warn("Config file not found at path specified by app.config: {}", configPath);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDatabaseTypes() {
        logger.debug("Fetching database types from configuration");
        Map<String, Object> databases = (Map<String, Object>) config.get("databases");
        if (databases == null) return Collections.emptyMap();
        
        Map<String, Object> types = (Map<String, Object>) databases.get("types");
        return types != null ? types : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDatabaseConfig(String dbType) {
        logger.debug("Fetching database config for type: {}", dbType);
        if (!isValidDbType(dbType)) {
            String errorMessage = "Invalid database type: " + dbType;
            logger.error(errorMessage);
            throw new DatabaseException(errorMessage, ErrorType.CONFIG_INVALID);
        }
        
        Map<String, Object> types = getDatabaseTypes();
        return new HashMap<>((Map<String, Object>) types.get(dbType.toLowerCase()));
    }

    @SuppressWarnings("unchecked")
    public String getJdbcClientTemplate(String dbType, String templateName) {
        logger.debug("Fetching JDBC client template for dbType: {}, templateName: {}", dbType, templateName);
        Map<String, Object> dbConfig = getDatabaseConfig(dbType);
        Map<String, Object> templates = (Map<String, Object>) dbConfig.get("templates");
        if (templates == null) return null;
        
        Map<String, Object> jdbcTemplates = (Map<String, Object>) templates.get("jdbc");
        if (jdbcTemplates == null) return null;
        
        Object template = jdbcTemplates.get(templateName);
        if (template == null) {
            template = jdbcTemplates.get("defaultTemplate");
        }
        if (template == null) {
            template = jdbcTemplates.get("default");
        }
        
        return template != null ? template.toString() : null;
    }

    public String getSqlTemplate(String dbType, String templateName) {
        logger.debug("Fetching SQL template for dbType: {}, templateName: {}", dbType, templateName);
        return getDatabaseTemplate(dbType, "sql", templateName);
    }

    @SuppressWarnings("unchecked")
    public String getDatabaseTemplate(String dbType, String category, String templateName) {
        logger.debug("Fetching database template for dbType: {}, category: {}, templateName: {}", dbType, category, templateName);
        Map<String, Object> dbConfig = getDatabaseConfig(dbType);
        Map<String, Object> templates = (Map<String, Object>) dbConfig.get("templates");
        if (templates == null) return null;
        
        Map<String, Object> categoryTemplates = (Map<String, Object>) templates.get(category);
        if (categoryTemplates == null) return null;
        
        Object template = categoryTemplates.get(templateName);
        return template != null ? template.toString() : null;
    }

    public void setRuntimeProperty(String key, String value) {
        logger.debug("Setting runtime property: {} = {}", key, value);
        runtimeProperties.put(key, value);
    }

    public String getRuntimeProperty(String key) {
        logger.debug("Getting runtime property for key: {}", key);
        return runtimeProperties.get(key);
    }

    public Map<String, String> getRuntimeProperties() {
        logger.debug("Getting all runtime properties");
        return new ConcurrentHashMap<>(runtimeProperties);
    }

    public boolean isValidDbType(String dbType) {
        logger.debug("Checking if valid dbType: {}", dbType);
        return dbType != null && getDatabaseTypes().containsKey(dbType.toLowerCase());
    }

    public int getDefaultPort(String dbType) {
        logger.debug("Getting default port for dbType: {}", dbType);
        Map<String, Object> dbConfig = getDatabaseConfig(dbType);
        Object port = dbConfig.get("defaultPort");
        return port instanceof Number number ? number.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getDatabaseProperties(String dbType) {
        logger.debug("Getting database properties for dbType: {}", dbType);
        Map<String, Object> dbConfig = getDatabaseConfig(dbType);
        Map<String, Object> properties = (Map<String, Object>) dbConfig.get("properties");
        
        if (properties == null) {
            return Collections.emptyMap();
        }
        
        // Convert all values to strings as required by the database properties
        Map<String, String> stringProperties = new HashMap<>();
        properties.forEach((key, value) -> stringProperties.put(key, value.toString()));
        return stringProperties;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getLoggingConfig() {
        logger.debug("Getting logging configuration");
        Map<String, Object> logging = (Map<String, Object>) config.get("logging");
        if (logging == null) {
            return Collections.emptyMap();
        }
        
        Map<String, String> loggingConfig = new HashMap<>();
        flattenMap("", logging, loggingConfig);
        return loggingConfig;
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(String prefix, Map<String, Object> map, Map<String, String> result) {
        logger.debug("Flattening map with prefix: {}", prefix);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value != null ? value.toString() : "");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getSpringConfig() {
        logger.debug("Getting spring configuration");
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        if (spring == null) {
            return Collections.emptyMap();
        }
        
        Map<String, String> springConfig = new HashMap<>();
        flattenMap("", spring, springConfig);
        return springConfig;
    }

    public String getApplicationName() {
        logger.debug("Getting application name from spring config");
        Map<String, String> springConfig = getSpringConfig();
        return springConfig.getOrDefault("application.name", "");
    }

    /**
     * Returns the root-level 'vault' configuration as a map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getVaultConfig() {
        logger.info("Getting root-level vault configuration");
        Object vaultObj = config.get("vault");
        if (vaultObj instanceof Map) {
            return new HashMap<>((Map<String, Object>) vaultObj);
        }
        return Collections.emptyMap();
    }
}