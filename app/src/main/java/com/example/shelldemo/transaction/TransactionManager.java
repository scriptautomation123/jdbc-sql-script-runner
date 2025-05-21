package com.example.shelldemo.transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database transactions to ensure proper commit/rollback semantics.
 * Extracts transaction management from other classes for better separation of concerns.
 */
public class TransactionManager {
    private static final Logger logger = LogManager.getLogger(TransactionManager.class);
    
    private final Connection connection;
    
    public TransactionManager(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Executes an operation within a transaction boundary.
     * Automatically commits on success or rolls back on failure.
     *
     * @param operation The SQL operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws SQLException if a database access error occurs
     */
    public <T> T executeInTransaction(SqlOperation<T> operation) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            T result = operation.execute(connection);
            connection.commit();
            logger.debug("Transaction committed successfully");
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
                logger.warn("Transaction rolled back due to error: {}", e.getMessage());
            } catch (SQLException rollbackEx) {
                logger.error("Failed to roll back transaction", rollbackEx);
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.warn("Failed to restore auto-commit setting", e);
            }
        }
    }
    
    /**
     * Functional interface for operations to be executed within a transaction.
     */
    @FunctionalInterface
    public interface SqlOperation<T> {
        /**
         * Executes a SQL operation.
         *
         * @param connection The database connection
         * @return The result of the operation
         * @throws SQLException if a database access error occurs
         */
        T execute(Connection connection) throws SQLException;
    }
}
