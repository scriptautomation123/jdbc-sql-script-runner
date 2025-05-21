package com.example.shelldemo.parser;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.ParseException;
import com.example.shelldemo.spi.DatabaseVendor;

/**
 * A utility to parse SQL files concurrently using Virtual Threads.
 * Demonstrates Java 21 features for efficient concurrent processing.
 */
public class ConcurrentSqlParser {
    private static final Logger logger = LogManager.getLogger(ConcurrentSqlParser.class);

    private ConcurrentSqlParser() {
        // Utility class - no instantiation
    }
    
    /**
     * Parses multiple SQL files concurrently using Java 21 Virtual Threads.
     * 
     * @param sqlFiles Array of SQL script files to parse
     * @param onComplete Callback function to execute when all parsing is complete
     * @param vendor Database vendor
     * @return Map of file names to their parsed statements
     */
    public static Map<String, Map<Integer, String>> parseFilesAsync(
            File[] sqlFiles, 
            Consumer<Map<String, Map<Integer, String>>> onComplete,
            DatabaseVendor vendor) {
        
        Map<String, Map<Integer, String>> results = new ConcurrentHashMap<>();
        
        // Use try-with-resources with virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Create a future for each file
            Future<?>[] futures = new Future<?>[sqlFiles.length];
            
            for (int i = 0; i < sqlFiles.length; i++) {
                final File file = sqlFiles[i];
                
                futures[i] = executor.submit(() -> {
                    try {
                        logger.info("Parsing SQL file: {}", file.getName());
                        Map<Integer, String> statements = SqlScriptParser.parseSqlFile(file, vendor);
                        results.put(file.getName(), statements);
                        logger.info("Completed parsing file: {}", file.getName());
                    } catch (DatabaseException e) {
                        logger.error("Error parsing file: {}", file.getName(), e);
                        throw e;
                    }
                });
            }
            
            // Wait for all parsing tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread was interrupted during SQL parsing", e);
                    throw new DatabaseException("SQL parsing was interrupted", 
                            e, ErrorType.PARSE_SQL);
                } catch (Exception e) {
                    logger.error("Error in concurrent SQL parsing", e);
                    throw new ParseException("Failed to parse SQL files concurrently", e);
                }
            }
            
            // Execute callback if provided
            if (onComplete != null) {
                onComplete.accept(results);
            }
            
            return results;
        }
    }
}
