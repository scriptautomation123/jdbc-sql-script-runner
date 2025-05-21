package com.example.shelldemo.examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.config.DatabaseConfig;
import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.spi.DatabaseVendor;
import com.example.shelldemo.spi.VendorRegistry;
import com.example.shelldemo.util.LoggingUtils;
import com.example.shelldemo.config.ConfigurationService;

/**
 * Example demonstrating how to use the enhanced DatabaseVendor implementation.
 * This shows database connection creation with vendor-specific configurations.
 */
public class DatabaseVendorExample {
    private static final Logger logger = LogManager.getLogger(DatabaseVendorExample.class);

    public static void main(String[] args) {
        // Example database configurations
        Map<String, DatabaseConfig> configs = Map.of(
            "oracle-prod", new DatabaseConfig(
                "oracle", "db-oracle.example.com", 1521, "PRODDB", 
                "app_user", "password", "thin-service"),
                
            "postgres-dev", new DatabaseConfig(
                "postgresql", "localhost", 5432, "dev_db", 
                "postgres", "postgres", null),
                
            "mysql-test", new DatabaseConfig(
                "mysql", "db-mysql.example.com", 3306, "test_db", 
                "test_user", "password", "serverTimezone=UTC"),
                
            "sqlserver-qa", new DatabaseConfig(
                "sqlserver", "10.0.0.25", 1433, "qa_db", 
                "sa", "Password123!", "applicationIntent=ReadOnly")
        );
        
        // Example for each database type
        for (var entry : configs.entrySet()) {
            String dbName = entry.getKey();
            DatabaseConfig config = entry.getValue();
            
            LoggingUtils.info(logger, "Connecting to {}", dbName);
            
            try {
                // Get the vendor implementation
                Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor(config.vendor());
                
                if (vendorOpt.isEmpty()) {
                    LoggingUtils.error(logger, "Unknown database vendor: {}", config.vendor());
                    continue;
                }
                
                DatabaseVendor vendor = vendorOpt.get();
                
                // Log connection details
                LoggingUtils.info(logger, "Using driver: {}", vendor.getDriverClassName());
                
                // Build connection URL
                String url = vendor.buildConnectionUrl(
                    config.host(), 
                    config.port(), 
                    config.database(), 
                    config.connectionType()
                );
                
                LoggingUtils.info(logger, "Connection URL: {}", url);
                
                // Get default properties and add credentials
                Properties props = vendor.getDefaultConnectionProperties();
                props.setProperty("user", config.username());
                props.setProperty("password", config.password());
                
                // Create the connection
                try {
                    // Load the driver class
                    Class.forName(vendor.getDriverClassName());
                    
                    // In a real application, connections would be pooled
                    try (Connection connection = DriverManager.getConnection(url, props)) {
                        // Initialize the connection with vendor-specific settings
                        vendor.initializeConnection(connection);
                        
                        // Validate the connection
                        boolean valid = vendor.validateConnection(connection);
                        LoggingUtils.info(logger, "Connection is valid: {}", valid);
                        
                        // Use the connection...
                        // In a real application, you would use DatabaseContext and executor components here
                        LoggingUtils.info(logger, "Successfully connected to {}", dbName);
                    }
                } catch (ClassNotFoundException e) {
                    LoggingUtils.error(logger, "Driver not found: {}", vendor.getDriverClassName(), e);
                } catch (SQLException e) {
                    LoggingUtils.error(logger, "Error connecting to {}: {}", dbName, e.getMessage(), e);
                }
            } catch (Exception e) {
                LoggingUtils.error(logger, "Unexpected error with {}: {}", dbName, e.getMessage(), e);
            }
            
            System.out.println("-".repeat(50));
        }
        
        // Complete DatabaseContext example using the vendor
        databaseContextExample();
    }
    
    private static void databaseContextExample() {
        LoggingUtils.info(logger, "DatabaseContext Example");
        
        // Get a vendor
        Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor("postgresql");
        if (vendorOpt.isEmpty()) {
            LoggingUtils.error(logger, "PostgreSQL vendor not available");
            return;
        }
        DatabaseVendor vendor = vendorOpt.get();
        // Create a database config
        DatabaseConfig config = new DatabaseConfig(
            "postgresql", "localhost", 5432, "sampledb", 
            "postgres", "postgres", null
        );
        // In a real application, you would get a connection from a pool
        String url = vendor.buildConnectionUrl(
            config.host(), config.port(), config.database(), config.connectionType()
        );
        Properties props = vendor.getDefaultConnectionProperties();
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        try {
            // Load driver
            Class.forName(vendor.getDriverClassName());
            // Create connection
            Connection connection = DriverManager.getConnection(url, props);
            vendor.initializeConnection(connection);
            // Create DatabaseContext
            ConfigurationService configService = new com.example.shelldemo.config.ConfigHolderAdapter();
            DatabaseContext context = new DatabaseContext.Builder()
                .connection(connection)
                .dbType(vendor.getVendorName())
                .vendor(vendor)
                .configService(configService)
                .build();
            LoggingUtils.info(logger, "DatabaseContext created successfully: {}", context);
            // In a real application, you would use this context with the executor components
            // For example:
            // QueryExecutor queryExecutor = new QueryExecutor();
            // List<Map<String, Object>> results = queryExecutor.executeQuery(context, "SELECT * FROM users");
            // Don't forget to close the connection when done
            connection.close();
        } catch (ClassNotFoundException e) {
            LoggingUtils.error(logger, "Driver not found: {}", vendor.getDriverClassName(), e);
        } catch (SQLException e) {
            LoggingUtils.error(logger, "SQL Error: {}", e.getMessage(), e);
        } catch (Exception e) {
            LoggingUtils.error(logger, "Unexpected error: {}", e.getMessage(), e);
        }
    }
}
