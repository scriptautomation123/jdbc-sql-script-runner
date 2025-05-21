package com.example.shelldemo.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.parser.SqlStatement;
import com.example.shelldemo.transaction.TransactionManager;
import com.example.shelldemo.util.ExceptionUtils;
import com.example.shelldemo.util.LoggingUtils;

/**
 * Executor for DDL statements (CREATE, ALTER, DROP).
 */
public final class DdlExecutor implements SqlExecutor {
    private final DatabaseContext context;
    
    /**
     * Creates a new DdlExecutor.
     *
     * @param context The database context
     */
    public DdlExecutor(DatabaseContext context) {
        this.context = context;
    }
    
    @Override
    public Boolean execute(SqlStatement statement) throws SQLException {
        Logger logger = context.getLogger();
        String user = null;
        try { user = context.getConnection().getMetaData().getUserName(); } catch (Exception ignore) {}
        ThreadContext.put("operation", "DDL");
        ThreadContext.put("dbType", context.getDbType());
        if (user != null) ThreadContext.put("user", user);
        logger.debug("Entering DdlExecutor.execute with SQL: {}", statement.getText());
        if (!(statement instanceof SqlStatement.DdlStatement)) {
            throw new IllegalArgumentException("Expected DdlStatement but got " + statement.getClass().getSimpleName());
        }
        String sql = statement.getText();
        Connection connection = context.getConnection();
        LoggingUtils.logOperation(logger, "DDL operation", sql);
        LoggingUtils.logSql(logger, sql);
        try {
            boolean result = executeDdl(sql, connection);
            logger.debug("Exiting DdlExecutor.execute with result: {}", result);
            return result;
        } catch (SQLException e) {
            throw ExceptionUtils.handleSQLException(
                e, "execute DDL statement", ErrorType.OP_DDL, logger);
        } finally {
            ThreadContext.clearAll();
        }
    }
    
    /**
     * Executes a DDL statement.
     *
     * @param sql The SQL statement
     * @param connection The database connection
     * @return true if the statement was successfully executed
     * @throws SQLException if a database access error occurs
     */
    private boolean executeDdl(String sql, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            return stmt.execute();
        }
    }
    
    /**
     * Executes a DDL statement within a transaction.
     * Note: Some database systems do not support DDL in transactions.
     *
     * @param sql The SQL statement
     * @return true if the statement was successfully executed
     * @throws SQLException if a database access error occurs
     */
    public boolean executeInTransaction(String sql) throws SQLException {
        TransactionManager txManager = context.getTransactionManager();
        return txManager.executeInTransaction(conn -> executeDdl(sql, conn));
    }
    
    @Override
    public DatabaseContext getContext() {
        return context;
    }
}
