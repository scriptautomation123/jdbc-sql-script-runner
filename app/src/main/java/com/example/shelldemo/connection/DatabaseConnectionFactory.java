package com.example.shelldemo.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;


import com.example.shelldemo.exception.ConfigurationException;
import com.example.shelldemo.spi.DatabaseVendor;
import com.example.shelldemo.spi.VendorRegistry;
import com.example.shelldemo.config.ConfigurationHolder;

/**
 * Factory for creating database connections using the vendor SPI system.
 * Uses Java 21 pattern matching for type checks and error handling.
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LogManager.getLogger(DatabaseConnectionFactory.class);
    private final JdbcDriverLoader driverLoader;

    /**
     * Creates a new database connection factory.
     */
    public DatabaseConnectionFactory() {
        logger.debug("Entering DatabaseConnectionFactory constructor");
        this.driverLoader = new JdbcDriverLoader();
        logger.debug("DatabaseConnectionFactory initialized");
        logger.debug("Exiting DatabaseConnectionFactory constructor");
    }

    /**
     * Creates a connection using a pre-configured ConnectionConfig.
     * 
     * @param config The validated connection configuration
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(ConnectionConfig config) throws SQLException {
        ThreadContext.put("operation", "connect");
        ThreadContext.put("dbType", config.dbType());
        ThreadContext.put("host", config.host());
        ThreadContext.put("user", config.username());
        logger.info("Creating database connection for type: {}, host: {}", config.dbType(), config.host());
        try {
            Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor(config.dbType());
            
            if (vendorOpt.isEmpty()) {
                throw new ConfigurationException(
                    "Unsupported database type: " + config.dbType()
                );
            }
            
            DatabaseVendor vendor = vendorOpt.get();
            
            driverLoader.loadDriverForVendor(vendor.getVendorName());
            
            String url;
            if ("thin-ldap".equalsIgnoreCase(config.connectionType())) {
                url = this.buildConnectionUrl(config);
            } else {
                url = vendor.buildConnectionUrl(
                    config.host(), 
                    config.port(), 
                    config.serviceName(), 
                    config.connectionType()
                );
            }
            
            logger.debug("Using connection URL: {}", url);
            
            Properties props = buildConnectionProperties(vendor, config);
            if (logger.isDebugEnabled()) {
                logger.debug("Connection properties configured: {}", 
                    props.stringPropertyNames().stream()
                        .filter(key -> !key.contains("password"))
                        .map(key -> key + "=" + props.getProperty(key))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none"));
            }
            
            Connection conn = DriverManager.getConnection(url, props);
            vendor.initializeConnection(conn);
            logger.info("Successfully established connection to {} database at {}:{}", 
                vendor.getVendorName(), config.host(), config.port());
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to create database connection: {}", e.getMessage(), e);
            throw e;
        } finally {
            ThreadContext.clearAll();
        }
    }

    /**
     * Creates a connection using builder pattern.
     * 
     * @param configBuilder A lambda that configures the connection
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(ConnectionConfigBuilder configBuilder) throws SQLException {
        logger.debug("Creating connection using builder pattern");
        ConnectionConfig config = configBuilder.configure(ConnectionConfig.builder()).build();
        return createConnection(config);
    }

    /**
     * Loads a JDBC driver from the specified path before creating a connection.
     * 
     * @param driverPath Path to the JDBC driver
     * @param config The connection configuration
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(String driverPath, ConnectionConfig config) throws SQLException {
        if (driverPath != null && !driverPath.isEmpty()) {
            logger.info("Loading JDBC driver from path: {}", driverPath);
            driverLoader.loadDriver(driverPath);
            logger.debug("JDBC driver loaded successfully");
        }
        return createConnection(config);
    }

    public String buildConnectionUrl(ConnectionConfig config) {
        logger.debug("Entering buildConnectionUrl with config: {}", config);
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(config.dbType());

        @SuppressWarnings("unchecked")
        Map<String, Object> templates = (Map<String, Object>) dbmsConfig.get("templates");
        if (templates != null) {
            logger.info("Available templates configuration:");
            templates.forEach((key, value) -> logger.info("  {} -> {}", key, value));
        }
      
        @SuppressWarnings("unchecked")
        Map<String, Object> jdbc = templates != null ? (Map<String, Object>) templates.get("jdbc") : null;
        
        if ("thin-ldap".equalsIgnoreCase(config.connectionType())) {
            return buildLdapConnectionUrl(config, templates, jdbc);
        } else {
            String urlTemplate = jdbc != null ? (String) jdbc.get("defaultTemplate") : null;
            if (urlTemplate == null) {
                urlTemplate = jdbc != null ? (String) jdbc.get("default") : null;
            }
            if (urlTemplate == null) {
                throw new ConfigurationException(
                    String.format("Missing URL template for database type: %s", config.dbType())
                );
            }

            String url = String.format(
                urlTemplate,
                config.host(),
                config.port(),
                config.serviceName()
            );
            logger.debug("Built connection URL: {}", url);
            logger.debug("Exiting buildConnectionUrl with url: {}", url);
            return url;
        }
    }

    private String buildLdapConnectionUrl(ConnectionConfig config, Map<String, Object> templates, Map<String, Object> jdbc) {
        String urlTemplate = jdbc != null ? (String) jdbc.get("ldap") : null;
        if (urlTemplate == null) {
            throw new ConfigurationException("Missing LDAP URL template for Oracle");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ldapConfig = (Map<String, Object>) templates.get("ldap");
        if (ldapConfig == null) {
            throw new ConfigurationException("Missing LDAP config for Oracle");
        }

        @SuppressWarnings("unchecked")
        java.util.List<String> servers = (java.util.List<String>) ldapConfig.get("servers");
        String context = (String) ldapConfig.get("context");
        Object portObj = ldapConfig.get("port");
        int port = (portObj instanceof Number number) ? number.intValue() : 389;
        String service = config.serviceName();

        if (servers == null || servers.isEmpty() || context == null || service == null) {
            throw new ConfigurationException("Incomplete LDAP configuration for Oracle");
        }

        String hosts = servers.stream()
            .map(server -> String.format(urlTemplate, server, port, service, context))
            .reduce((a, b) -> a + " " + b)
            .orElseThrow();

        String url = "jdbc:oracle:thin:@" + hosts;
        logger.debug("Built LDAP connection URL: {}", url);
        return url;
    }

    /**
     * Builds connection properties combining vendor defaults with configuration.
     * 
     * @param vendor The database vendor
     * @param config The connection configuration
     * @return Properties for the connection
     */
    protected Properties buildConnectionProperties(DatabaseVendor vendor, ConnectionConfig config) {
        // Start with vendor defaults
        Properties props = vendor.getDefaultConnectionProperties();
        
        // Add authentication properties
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        
        // Get any additional database-specific properties from configuration
        try {
            Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(config.dbType());
            Map<String, Object> connProps = ConnectionConfig.getConfigMap(dbmsConfig, "properties");
            if (connProps != null) {
                logger.debug("Applying {} database-specific connection properties", connProps.size());
                for (Map.Entry<String, Object> entry : connProps.entrySet()) {
                    props.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (RuntimeException e) {
            // Log as debug since this is optional
            logger.debug("No additional connection properties found for vendor: {}", vendor.getVendorName());
        }
        
        return props;
    }

    /**
     * Lists supported database types.
     * 
     * @return Set of supported database vendor names
     */
    public Set<String> getSupportedDatabaseTypes() {
        return VendorRegistry.getAllVendors().values().stream().map(DatabaseVendor::getVendorName).collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Checks if a database type is supported.
     * 
     * @param dbType Database type to check
     * @return true if the database type is supported
     */
    public boolean isSupported(String dbType) {
        return VendorRegistry.getVendor(dbType).isPresent();
    }

    @FunctionalInterface
    public interface ConnectionConfigBuilder {
        ConnectionConfig.Builder configure(ConnectionConfig.Builder builder);
    }
}
