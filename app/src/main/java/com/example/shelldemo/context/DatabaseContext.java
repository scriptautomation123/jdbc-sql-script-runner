package com.example.shelldemo.context;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.spi.DatabaseVendor;
import com.example.shelldemo.sqlhandling.DatabaseErrorHandler;
import com.example.shelldemo.transaction.TransactionManager;

/**
 * Central context class that holds all resources needed for database operations.
 * Provides a unified access point for connection, configuration, and vendor-specific features.
 */
public class DatabaseContext implements AutoCloseable {
    private final Connection connection;
    private final DatabaseVendor vendor;
    private final ConfigurationService configService;
    private final TransactionManager transactionManager;
    private final DatabaseErrorHandler errorHandler;
    private final Logger logger;
    private final String dbType;
    
    private DatabaseContext(Builder builder) {
        this.connection = builder.connection;
        this.vendor = builder.vendor;
        this.configService = builder.configService;
        this.dbType = builder.dbType;
        this.logger = LogManager.getLogger(DatabaseContext.class);
        this.errorHandler = new DatabaseErrorHandler(vendor.getVendorName());
        this.transactionManager = new TransactionManager(connection);
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public DatabaseVendor getVendor() {
        return vendor;
    }
    
    public ConfigurationService getConfigService() {
        return configService;
    }
    
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    public DatabaseErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    public String getDbType() {
        return dbType;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            logger.debug("Database connection closed");
        }
    }
    
    /**
     * Builder for creating DatabaseContext instances.
     */
    public static class Builder {
        private Connection connection;
        private DatabaseVendor vendor;
        private ConfigurationService configService;
        private String dbType;
        
        public Builder connection(Connection connection) {
            this.connection = connection;
            return this;
        }
        
        public Builder vendor(DatabaseVendor vendor) {
            // Use the adapter to ensure consistent vendor behavior
            this.vendor = new com.example.shelldemo.spi.DatabaseVendorAdapter(vendor, configService);
            return this;
        }
        
        public Builder configService(ConfigurationService configService) {
            this.configService = configService;
            return this;
        }
        
        public Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }
        
        public DatabaseContext build() {
            validate();
            return new DatabaseContext(this);
        }
        
        private void validate() {
            if (connection == null) {
                throw new IllegalStateException("Connection cannot be null");
            }
            if (vendor == null) {
                throw new IllegalStateException("DatabaseVendor cannot be null");
            }
            if (configService == null) {
                throw new IllegalStateException("ConfigurationService cannot be null");
            }
            if (dbType == null || dbType.isBlank()) {
                throw new IllegalStateException("Database type cannot be null or blank");
            }
        }
    }
}
