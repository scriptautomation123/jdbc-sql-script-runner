package com.example.shelldemo.examples;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.UnifiedDatabaseOperation;
import com.example.shelldemo.UnifiedDatabaseOperationBuilder;
import com.example.shelldemo.config.ConfigHolderAdapter;
import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.executor.DmlExecutor;
import com.example.shelldemo.executor.QueryExecutor;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.parser.SqlStatementFactory;
import com.example.shelldemo.spi.DatabaseVendor;
import com.example.shelldemo.spi.VendorRegistry;
import com.example.shelldemo.sqlhandling.VirtualThreadBatchExecutor;
import com.example.shelldemo.sqlhandling.VirtualThreadBatchExecutor.ProcessingMode;

/**
 * Example demonstrating the usage of the refactored components.
 */
public class RefactoringExample {
    private static final Logger logger = LogManager.getLogger(RefactoringExample.class);
    
    public static void main(String[] args) {
        // Example 1: Using the new API through the UnifiedDatabaseOperation2 facade
        exampleUsingFacade();
        
        // Example 2: Using specialized executors directly
        exampleUsingExecutors();
        
        // Example 3: Using virtual thread batch processing
        exampleUsingVirtualThreads();
    }
    
    /**
     * Example using the UnifiedDatabaseOperation2 facade.
     */
    private static void exampleUsingFacade() {
        logger.info("Example 1: Using the UnifiedDatabaseOperation2 facade");
        
        try {
            // Get vendor from registry
            Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor("oracle");
            if (vendorOpt.isEmpty()) {
                logger.error("Oracle vendor not found");
                return;
            }
            DatabaseVendor vendor = vendorOpt.get();
            
            // Create operation with builder
            try (UnifiedDatabaseOperation db = new UnifiedDatabaseOperationBuilder()
                    .host("localhost")
                    .port(1521)
                    .username("user")
                    .password("pass")
                    .dbType("oracle")
                    .serviceName("XE")
                    .vendor(vendor)
                    .build()) {
                
                // Execute a query
                List<Map<String, Object>> results = db.executeQuery("SELECT * FROM employees");
                logger.info("Query returned {} rows", results.size());
                
                // Execute an update
                int rowsAffected = db.executeUpdate("UPDATE employees SET salary = 5000 WHERE id = 1");
                logger.info("Update affected {} rows", rowsAffected);
            }
            
        } catch (Exception e) {
            logger.error("Error in example 1: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Example using specialized executors directly.
     */
    private static void exampleUsingExecutors() {
        logger.info("Example 2: Using specialized executors directly");
        
        try {
            // Create connection
            DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory();
            ConnectionConfig connectionConfig = new ConnectionConfig(
                    "localhost", 1521, "user", "pass", "XE", "oracle", "thin");
            Connection connection = connectionFactory.createConnection(connectionConfig);
            
            // Get vendor
            Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor("oracle");
            if (vendorOpt.isEmpty()) {
                logger.error("Oracle vendor not found");
                return;
            }
            DatabaseVendor vendor = vendorOpt.get();
            
            // Create context
            ConfigurationService configService = new ConfigHolderAdapter();
            DatabaseContext context = new DatabaseContext.Builder()
                    .connection(connection)
                    .dbType("oracle")
                    .configService(configService)
                    .build();
            
            // Create executors
            QueryExecutor queryExecutor = new QueryExecutor(context);
            DmlExecutor dmlExecutor = new DmlExecutor(context);
            
            // Create statement factory
            SqlStatementFactory statementFactory = new SqlStatementFactory(
                    Map.of("oracle", vendor));
            
            // Create and execute statements
            SqlStatement queryStmt = statementFactory.createStatement(
                    "SELECT * FROM employees", "oracle");
            List<Map<String, Object>> results = (List<Map<String, Object>>) 
                    queryExecutor.execute(queryStmt);
            logger.info("Query returned {} rows", results.size());
            
            SqlStatement updateStmt = statementFactory.createStatement(
                    "UPDATE employees SET salary = 6000 WHERE id = 2", "oracle");
            int rowsAffected = (Integer) dmlExecutor.execute(updateStmt);
            logger.info("Update affected {} rows", rowsAffected);
            
            // Close context (will close connection)
            context.close();
            
        } catch (Exception e) {
            logger.error("Error in example 2: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Example using virtual thread batch processing.
     */
    private static void exampleUsingVirtualThreads() {
        logger.info("Example 3: Using virtual thread batch processing");
        
        try {
            // Create connection
            DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory();
            ConnectionConfig connectionConfig = new ConnectionConfig(
                    "localhost", 1521, "user", "pass", "XE", "oracle", "thin");
            Connection connection = connectionFactory.createConnection(connectionConfig);
            
            // Get vendor
            Optional<DatabaseVendor> vendorOpt = VendorRegistry.getVendor("oracle");
            if (vendorOpt.isEmpty()) {
                logger.error("Oracle vendor not found");
                return;
            }
            DatabaseVendor vendor = vendorOpt.get();
            
            // Create context
            ConfigurationService configService = new ConfigHolderAdapter();
            DatabaseContext context = new DatabaseContext.Builder()
                    .connection(connection)
                    .dbType("oracle")
                    .configService(configService)
                    .build();
            
            // Create statement factory
            SqlStatementFactory statementFactory = new SqlStatementFactory(
                    Map.of("oracle", vendor));
            
            // Create batch executor
            VirtualThreadBatchExecutor batchExecutor = new VirtualThreadBatchExecutor(
                    context, statementFactory, 60, true);
            
            // Create statements
            List<String> statements = List.of(
                    "INSERT INTO employees VALUES (3, 'John', 4000)",
                    "INSERT INTO employees VALUES (4, 'Jane', 4500)",
                    "INSERT INTO employees VALUES (5, 'Bob', 5000)",
                    "UPDATE employees SET salary = 5500 WHERE salary < 5000"
            );
            
            // Execute batch
            int executed = batchExecutor.executeBatch(statements, ProcessingMode.CONCURRENT);
            logger.info("Batch executed {} statements", executed);
            
            // Close context (will close connection)
            context.close();
            
        } catch (Exception e) {
            logger.error("Error in example 3: {}", e.getMessage(), e);
        }
    }
}
