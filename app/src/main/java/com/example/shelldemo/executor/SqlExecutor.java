package com.example.shelldemo.executor;

import java.sql.SQLException;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.parser.SqlStatement;

/**
 * Base interface for all SQL executors.
 * Provides a common execution model for different types of SQL statements.
 */
public sealed interface SqlExecutor 
    permits QueryExecutor, DmlExecutor, ProcedureExecutor, DdlExecutor {
    
    /**
     * Executes a SQL statement and returns the result.
     * The result type depends on the executor implementation.
     *
     * @param statement The SQL statement to execute
     * @return The result of the execution
     * @throws SQLException if a database access error occurs
     */
    Object execute(SqlStatement statement) throws SQLException;
    
    /**
     * Gets the database context for this executor.
     *
     * @return The database context
     */
    DatabaseContext getContext();
    
    /**
     * Factory method to create the appropriate executor for a statement.
     *
     * @param statement The SQL statement
     * @param context The database context
     * @return An appropriate SqlExecutor instance
     */
    static SqlExecutor createFor(SqlStatement statement, DatabaseContext context) {
        return switch (statement) {
            case SqlStatement.QueryStatement q -> new QueryExecutor(context);
            case SqlStatement.DmlStatement d -> new DmlExecutor(context);
            case SqlStatement.ProcedureStatement p -> new ProcedureExecutor(context);
            case SqlStatement.DdlStatement d -> new DdlExecutor(context);
        };
    }
}
