package com.example.shelldemo.connection;

import java.util.Map;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.config.ConfigurationHolder;

/**
 * Immutable record for database connection configuration.
 * Uses Java 21 pattern matching and records for immutability.
 */
public record ConnectionConfig(
    String host,
    int port,
    String username,
    String password,
    String serviceName,
    String dbType,
    String connectionType
) {
    private static final Logger logger = LogManager.getLogger(ConnectionConfig.class);

    /**
     * Compact constructor for validation
     */
    public ConnectionConfig {
        if (dbType != null) {
            dbType = dbType.trim().toLowerCase();
        }
    }

    /**
     * Validates and enriches the configuration with default values.
     * 
     * @return A new ConnectionConfig with enriched values
     * @throws DatabaseException if validation fails
     */
    public ConnectionConfig validateAndEnrich() {
        validateRequiredFields();
        
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(dbType);
        
        if (dbmsConfig == null || dbmsConfig.isEmpty()) {
            String errorMessage = "Invalid or unsupported database type: " + dbType;
            logger.error(errorMessage);
            throw new DatabaseException(errorMessage, ErrorType.CONFIG_INVALID);
        }

        Map<String, Object> defaults = getConfigMap(dbmsConfig, "defaults");
        int enrichedPort = enrichPort(defaults, port);
        String enrichedConnType = enrichConnectionType(defaults, connectionType);
        
        return new ConnectionConfig(
            host, 
            enrichedPort, 
            username, 
            password, 
            serviceName, 
            dbType, 
            enrichedConnType
        );
    }

    /**
     * Helper method for getting configuration maps using pattern matching
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfigMap(Object obj, String key) {
        // Using Java 21 pattern matching for instanceof
        if (obj instanceof Map<?,?> map) {
            Object config = map.get(key);
            if (config instanceof Map<?,?> configMap) {
                return (Map<String, Object>)configMap;
            }
            
            // If the key doesn't exist, return an empty map instead of throwing an exception
            if ("defaults".equals(key)) {
                logger.warn("Configuration section '{}' is missing - using empty defaults", key);
                return new HashMap<>();
            }
        }
        
        throw new DatabaseException(
            String.format("Required configuration section '%s' is missing or invalid", key),
            ErrorType.CONFIG_NOT_FOUND
        );
    }

    /**
     * Calculate port enrichment without mutation
     */
    private int enrichPort(Map<String, Object> defaults, int currentPort) {
        if (defaults.containsKey("port") && currentPort <= 0) {
            Object portValue = defaults.get("port");
            
            // Using pattern matching for instanceof
            if (portValue instanceof Number number) {
                return number.intValue();
            } else {
                throw new DatabaseException(
                    String.format("Invalid port configuration. Expected number but got: %s", 
                        portValue != null ? portValue.getClass().getSimpleName() : "null"
                    ),
                    ErrorType.CONFIG_INVALID
                );
            }
        }
        return currentPort;
    }

    /**
     * Calculate connection type enrichment without mutation
     */
    private String enrichConnectionType(Map<String, Object> defaults, String currentConnType) {
        if (defaults.containsKey("connection-type") && currentConnType == null) {
            Object connType = defaults.get("connection-type");
            
            // Using pattern matching for instanceof
            if (connType instanceof String defaultConnType) {
                logger.info("No connection type specified, using default: {}", defaultConnType);
                return defaultConnType;
            } else {
                throw new DatabaseException(
                    String.format("Invalid connection-type configuration. Expected string but got: %s",
                        connType != null ? connType.getClass().getSimpleName() : "null"
                    ),
                    ErrorType.CONFIG_INVALID
                );
            }
        }
        return currentConnType;
    }

    /**
     * Validates that all required fields are present and valid.
     * 
     * @throws DatabaseException if validation fails
     */
    private void validateRequiredFields() {
        StringBuilder errors = new StringBuilder();


        
        if (dbType == null || dbType.trim().isEmpty()) {
            errors.append("Database type must be specified. ");
        }
        // Only require host for thin connection type (not thin-ldap)
        if ("thin".equalsIgnoreCase(connectionType) && (host == null || host.trim().isEmpty())) {
            errors.append("Host must be specified for thin connection type. ");
        }
        // For thin-ldap, host is not required
        if (serviceName == null || serviceName.trim().isEmpty()) {
            errors.append("Service name must be specified. ");
        }
        if (username == null || username.trim().isEmpty()) {
            errors.append("Username must be specified. ");
        }
        if (password == null) {
            errors.append("Password must be specified. ");
        }
        if (errors.length() > 0) {
            throw new DatabaseException(errors.toString(), ErrorType.CONFIG_INVALID);
        }
    }
    
    /**
     * Creates a builder for fluent configuration creation.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating ConnectionConfig instances.
     * Uses Java 21 pattern matching in the build method.
     */
    public static class Builder {
        private String host;
        private int port;
        private String username;
        private String password;
        private String serviceName;
        private String dbType;
        private String connectionType;

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
        public Builder dbType(String dbType) { this.dbType = dbType; return this; }
        public Builder connectionType(String connectionType) { this.connectionType = connectionType; return this; }

        public ConnectionConfig build() {
            ConnectionConfig config = new ConnectionConfig(
                host, 
                port, 
                username, 
                password, 
                serviceName, 
                dbType, 
                connectionType
            );
            return config.validateAndEnrich();
        }
    }

    /**
     * Factory methods for common database configurations
     */
    public static ConnectionConfig forOracle(String host, int port, String serviceName, String username, String password) {
        return new ConnectionConfig(host, port, username, password, serviceName, "oracle", "thin");
    }
    
    public static ConnectionConfig forPostgreSQL(String host, int port, String database, String username, String password) {
        return new ConnectionConfig(host, port, username, password, database, "postgresql", null);
    }
    
    public static ConnectionConfig forMySql(String host, int port, String database, String username, String password) {
        return new ConnectionConfig(host, port, username, password, database, "mysql", null);
    }
    
    public static ConnectionConfig forSqlServer(String host, int port, String database, String username, String password) {
        return new ConnectionConfig(host, port, username, password, database, "sqlserver", null);
    }
}