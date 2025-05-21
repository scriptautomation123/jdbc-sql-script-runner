package com.example.shelldemo.sqlhandling;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.BatchProcessingException;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.parser.SqlStatementFactory;
import com.example.shelldemo.util.LoggingUtils;

/**
 * Enhanced batch executor that uses Java 21 virtual threads for efficient concurrent processing.
 */
public class VirtualThreadBatchExecutor {
    private static final Logger logger = LogManager.getLogger(VirtualThreadBatchExecutor.class);
    
    private final DatabaseContext context;
    private final SqlStatementFactory statementFactory;
    private final int timeout;
    private final boolean stopOnError;
    
    /**
     * Creates a new VirtualThreadBatchExecutor.
     *
     * @param context The database context
     * @param statementFactory The statement factory
     * @param timeout The timeout in seconds
     * @param stopOnError Whether to stop on error
     */
    public VirtualThreadBatchExecutor(
            DatabaseContext context, 
            SqlStatementFactory statementFactory,
            int timeout,
            boolean stopOnError) {
        this.context = context;
        this.statementFactory = statementFactory;
        this.timeout = timeout;
        this.stopOnError = stopOnError;
    }
    
    /**
     * Various processing modes for batch execution.
     */
    public enum ProcessingMode {
        SEQUENTIAL,  // Process statements sequentially
        CONCURRENT,  // Process statements concurrently with virtual threads
        TRANSACTION  // Process statements in a transaction
    }
    
    /**
     * Executes a batch of SQL statements.
     *
     * @param statements The SQL statements to execute
     * @param mode The processing mode
     * @return The number of statements executed
     * @throws SQLException if a database access error occurs
     */
    public int executeBatch(List<String> statements, ProcessingMode mode) throws SQLException {
        if (statements == null || statements.isEmpty()) {
            return 0;
        }
        
        LoggingUtils.logOperation(logger, "batch execution", 
                "Processing " + statements.size() + " statements in " + mode + " mode");
        
        long startTime = System.currentTimeMillis();
        
        try {
            return switch (mode) {
                case SEQUENTIAL -> executeSequentially(statements);
                case CONCURRENT -> executeConcurrently(statements);
                case TRANSACTION -> executeInTransaction(statements);
            };
        } finally {
            LoggingUtils.logOperationComplete(logger, "Batch execution", startTime);
        }
    }
    
    /**
     * Executes statements sequentially.
     */
    private int executeSequentially(List<String> statements) throws SQLException {
        Connection connection = context.getConnection();
        String dbType = context.getDbType();
        int executed = 0;
        
        for (int i = 0; i < statements.size(); i++) {
            String sql = statements.get(i);
            try {
                SqlStatement statement = statementFactory.createStatement(sql, dbType);
                executeSingleStatement(connection, statement);
                executed++;
            } catch (SQLException e) {
                logger.error("Error executing statement #{}: {}", i + 1, e.getMessage());
                if (stopOnError) {
                    throw new BatchProcessingException(
                            "Failed to execute statement #" + (i + 1), 
                            e, statements.size(), i);
                }
            }
        }
        
        return executed;
    }
    
    /**
     * Executes statements concurrently using virtual threads.
     */
    private int executeConcurrently(List<String> statements) throws SQLException {
        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger failedIndex = new AtomicInteger(-1);
        java.util.concurrent.atomic.AtomicReference<Exception> firstException = new java.util.concurrent.atomic.AtomicReference<>(null);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            statements.stream()
                .map(sql -> (Future<?>) executor.submit(() -> {
                    try {
                        Connection connection = context.getConnection();
                        SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
                        executeSingleStatement(connection, statement);
                        executed.incrementAndGet();
                    } catch (Exception e) {
                        int index = statements.indexOf(sql);
                        logger.error("Error executing statement #{}: {}", index + 1, e.getMessage());
                        failedIndex.compareAndSet(-1, index);
                        firstException.compareAndSet(null, e);
                        if (stopOnError) {
                            throw new RuntimeException(e);
                        }
                    }
                }))
                .toList();
            
            // Wait for all tasks to complete or timeout
            executor.shutdown();
            if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                throw new SQLException("Batch execution timed out after " + timeout + " seconds");
            }
            
            // Check for errors
            if (firstException.get() != null && stopOnError) {
                if (firstException.get() instanceof SQLException) {
                    throw (SQLException) firstException.get();
                } else {
                    throw new SQLException("Error in batch execution", firstException.get());
                }
            }
            
            // Check for exceptions
            if (firstException.get() != null && stopOnError) {
                if (firstException.get() instanceof SQLException sqlEx) {
                    throw sqlEx;
                } else {
                    throw new SQLException("Batch execution failed", firstException.get());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Batch execution was interrupted", e);
        }
        
        return executed.get();
    }
    
    /**
     * Executes statements in a transaction.
     */
    private int executeInTransaction(List<String> statements) throws SQLException {
        return context.getTransactionManager().executeInTransaction(conn -> {
            int executed = 0;
            for (int i = 0; i < statements.size(); i++) {
                String sql = statements.get(i);
                try {
                    SqlStatement statement = statementFactory.createStatement(sql, context.getDbType());
                    executeSingleStatement(conn, statement);
                    executed++;
                } catch (SQLException e) {
                    logger.error("Error executing statement #{}: {}", i + 1, e.getMessage());
                    throw new BatchProcessingException(
                            "Failed to execute statement #" + (i + 1), 
                            e, statements.size(), i);
                }
            }
            return executed;
        });
    }
    
    /**
     * Executes a single statement.
     */
    private void executeSingleStatement(Connection connection, SqlStatement statement) throws SQLException {
        LoggingUtils.logSql(logger, statement.getText());
        
        java.sql.Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute(statement.getText());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.warn("Error closing statement: {}", e.getMessage());
                }
            }
        }
    }
}
