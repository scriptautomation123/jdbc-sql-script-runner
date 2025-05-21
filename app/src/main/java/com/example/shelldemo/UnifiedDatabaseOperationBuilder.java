package com.example.shelldemo;

import java.sql.Connection;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.config.ConfigHolderAdapter;
import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.config.DatabaseConfig;
import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.spi.DatabaseVendor;

/**
 * Builder for UnifiedDatabaseOperation instances.
 * Provides a fluent API for creating database operations.
 */
public class UnifiedDatabaseOperationBuilder {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseOperationBuilder.class);
    
    private String host;
    private int port;
    private String username;
    private String password;
    private String dbType;
    private String serviceName;
    private String connectionType;
    private Connection existingConnection;
    private ConfigurationService configService;
    private DatabaseVendor vendor;
    private boolean transactional = false;

    /**
     * Sets the database host.
     *
     * @param host The host
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder host(String host) { 
        this.host = host; 
        return this; 
    }
    
    /**
     * Sets the database port.
     *
     * @param port The port
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder port(int port) { 
        this.port = port; 
        return this; 
    }
    
    /**
     * Sets the username.
     *
     * @param username The username
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder username(String username) { 
        this.username = username; 
        return this; 
    }
    
    /**
     * Sets the password.
     *
     * @param password The password
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder password(String password) { 
        this.password = password; 
        return this; 
    }
    
    /**
     * Sets the database type.
     *
     * @param dbType The database type
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder dbType(String dbType) { 
        this.dbType = dbType; 
        return this; 
    }
    
    /**
     * Sets the connection type.
     *
     * @param connectionType The connection type
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder connectionType(String connectionType) { 
        this.connectionType = connectionType; 
        return this; 
    }
    
    /**
     * Sets the service name or database name.
     *
     * @param serviceName The service name
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder serviceName(String serviceName) { 
        this.serviceName = serviceName; 
        return this; 
    }
    
    /**
     * Sets an existing connection to use instead of creating a new one.
     *
     * @param connection The existing connection
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder connection(Connection connection) {
        this.existingConnection = connection;
        return this;
    }
    
    /**
     * Sets a custom configuration service.
     *
     * @param configService The configuration service
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder configService(ConfigurationService configService) {
        this.configService = configService;
        return this; 
    }
    
    /**
     * Sets a specific database vendor implementation.
     *
     * @param vendor The database vendor implementation
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder vendor(DatabaseVendor vendor) {
        this.vendor = vendor;
        return this;
    }
    
    /**
     * Sets the transactional flag for DML execution.
     *
     * @param transactional Whether to use transactions for DML
     * @return This builder
     */
    public UnifiedDatabaseOperationBuilder transactional(boolean transactional) {
        this.transactional = transactional;
        return this;
    }
    
    /**
     * Builds a UnifiedDatabaseOperation2.
     *
     * @return A new UnifiedDatabaseOperation2
     * @throws DatabaseException if an error occurs during creation
     */
    public UnifiedDatabaseOperation build() {
        logger.debug("Entering UnifiedDatabaseOperationBuilder.build()");
        try {
            // Make sure we have a vendor
            if (vendor == null) {
                Objects.requireNonNull(dbType, "Database type is required when vendor is not specified");
                vendor = com.example.shelldemo.spi.VendorRegistry.getVendor(dbType)
                    .orElseThrow(() -> new IllegalArgumentException("No DatabaseVendor implementation found for type: " + dbType));
            }
            if (configService == null && vendor.getConfigService() != null) {
                configService = vendor.getConfigService();
            }
            if (configService == null) {
                configService = new ConfigHolderAdapter();
            }
            if (existingConnection == null) {
                try {
                    DatabaseConfig config = new DatabaseConfig(
                        dbType,
                        host,
                        port,
                        serviceName,
                        username,
                        password,
                        connectionType
                    );
                    UnifiedDatabaseOperation op = UnifiedDatabaseOperation.create(config, vendor, transactional);
                    logger.debug("Exiting UnifiedDatabaseOperationBuilder.build() with operation: {}", op);
                    return op;
                } catch (Exception e) {
                    logger.error("Failed to create database connection: {}", e.getMessage(), e);
                    throw new DatabaseException("Failed to create database connection", e, ErrorType.CONN_FAILED);
                }
            } else {
                Objects.requireNonNull(dbType, "Database type is required");
                DatabaseContext context = new DatabaseContext.Builder()
                    .connection(existingConnection)
                    .dbType(dbType)
                    .vendor(vendor)
                    .configService(configService)
                    .build();
                UnifiedDatabaseOperation op = new UnifiedDatabaseOperation(context, vendor, transactional);
                logger.debug("Exiting UnifiedDatabaseOperationBuilder.build() with operation: {}", op);
                return op;
            }
        } catch (Exception e) {
            logger.error("Failed to build UnifiedDatabaseOperation2: {}", e.getMessage(), e);
            if (e instanceof DatabaseException) {
                throw (DatabaseException) e;
            }
            throw new DatabaseException("Failed to build UnifiedDatabaseOperation2", e, ErrorType.CONN_FAILED);
        }
    }
}
