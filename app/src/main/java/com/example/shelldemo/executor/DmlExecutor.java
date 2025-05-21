package com.example.shelldemo.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.transaction.TransactionManager;
import com.example.shelldemo.util.ExceptionUtils;
import com.example.shelldemo.util.LoggingUtils;

/**
 * Executor for DML statements (INSERT, UPDATE, DELETE).
 */
public final class DmlExecutor implements SqlExecutor {
    private final DatabaseContext context;
    
    /**
     * Creates a new DmlExecutor.
     *
     * @param context The database context
     */
    public DmlExecutor(DatabaseContext context) {
        this.context = context;
    }
    
    @Override
    public Integer execute(SqlStatement statement) throws SQLException {
        Logger logger = context.getLogger();
        logger.debug("Entering DmlExecutor.execute with SQL: {}", statement.getText());
        if (!(statement instanceof SqlStatement.DmlStatement)) {
            throw new IllegalArgumentException("Expected DmlStatement but got " + statement.getClass().getSimpleName());
        }
        String sql = statement.getText();
        Connection connection = context.getConnection();
        LoggingUtils.logOperation(logger, "DML operation", sql);
        LoggingUtils.logSql(logger, sql);
        try {
            int result = executeUpdate(sql, connection);
            logger.debug("Exiting DmlExecutor.execute with result: {}", result);
            return result;
        } catch (SQLException e) {
            throw ExceptionUtils.handleSQLException(
                e, "execute DML statement", ErrorType.OP_UPDATE, logger);
        }
    }
    
    /**
     * Executes a DML statement and returns the number of affected rows.
     *
     * @param sql The SQL statement
     * @param connection The database connection
     * @return The number of affected rows
     * @throws SQLException if a database access error occurs
     */
    private int executeUpdate(String sql, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Executes a DML statement within a transaction.
     *
     * @param sql The SQL statement
     * @return The number of affected rows
     * @throws SQLException if a database access error occurs
     */
    public int executeInTransaction(String sql) throws SQLException {
        TransactionManager txManager = context.getTransactionManager();
        return txManager.executeInTransaction(conn -> executeUpdate(sql, conn));
    }
    
    @Override
    public DatabaseContext getContext() {
        return context;
    }
}
