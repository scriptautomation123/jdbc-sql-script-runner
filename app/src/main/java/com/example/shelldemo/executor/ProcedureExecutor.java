package com.example.shelldemo.executor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.example.shelldemo.context.DatabaseContext;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.parser.SqlScriptParser.ProcedureParam;

import com.example.shelldemo.parser.SqlStatement;

import com.example.shelldemo.util.ExceptionUtils;
import com.example.shelldemo.util.LoggingUtils;

/**
 * Executor for stored procedures and PL/SQL blocks.
 */
public record ProcedureExecutor(DatabaseContext context) implements SqlExecutor {
    
    @Override
    public Map<String, Object> execute(SqlStatement statement) throws SQLException {
        Logger logger = context.getLogger();
        logger.debug("Entering ProcedureExecutor.execute with SQL: {}", statement.getText());
        if (!(statement instanceof SqlStatement.ProcedureStatement)) {
            throw new IllegalArgumentException("Expected ProcedureStatement but got " + statement.getClass().getSimpleName());
        }
        String sql = statement.getText();
        Connection connection = context.getConnection();
        LoggingUtils.logOperation(logger, "procedure", sql);
        LoggingUtils.logSql(logger, sql);
        try {
            Map<String, Object> result = executeBlock(sql, connection);
            logger.debug("Exiting ProcedureExecutor.execute with result: {}", result);
            return result;
        } catch (SQLException e) {
            throw ExceptionUtils.handleSQLException(
                e, "execute procedure", ErrorType.OP_PROCEDURE, logger);
        }
    }
    
    /**
     * Executes a PL/SQL block directly.
     *
     * @param sql The PL/SQL block
     * @param connection The database connection
     * @return A map of output parameters and their values
     * @throws SQLException if a database access error occurs
     */
    private Map<String, Object> executeBlock(String sql, Connection connection) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.execute();
            return new HashMap<>();  // No output parameters for direct blocks
        }
    }
    
    /**
     * Calls a stored procedure with parameters.
     *
     * @param procedureName The name of the procedure
     * @param inParams Input parameters
     * @param outParams Output parameters
     * @return A map of output parameters and their values
     * @throws SQLException if a database access error occurs
     */
    public Map<String, Object> callProcedure(String procedureName,List<ProcedureParam> inParams,List<ProcedureParam> outParams) throws SQLException {
        Logger logger = context.getLogger();
        String user = null;
        try { user = context.getConnection().getMetaData().getUserName(); } catch (Exception ignore) {}
        ThreadContext.put("operation", "procedure");
        ThreadContext.put("procedureName", procedureName);
        ThreadContext.put("dbType", context.getDbType());
        if (user != null) ThreadContext.put("user", user);
        logger.debug("Entering ProcedureExecutor.callProcedure with procedureName: {}, inParams: {}, outParams: {}", procedureName, inParams, outParams);
        try {
            return doCallProcedure(context.getConnection(), procedureName, inParams, outParams);
        } catch (SQLException e) {
            throw ExceptionUtils.handleSQLException(
                e, "call procedure " + procedureName, ErrorType.OP_PROCEDURE, logger);
        } finally {
            ThreadContext.clearAll();
        }
    }
    

    private Map<String, Object> doCallProcedure(Connection connection, String procedureName,List<ProcedureParam> inParams,List<ProcedureParam> outParams) throws SQLException {
        
        StringBuilder callString = new StringBuilder("{call ").append(procedureName).append("(");
        
        int paramCount = (inParams != null ? inParams.size() : 0) + (outParams != null ? outParams.size() : 0);
        
        for (int i = 0; i < paramCount; i++) {
            callString.append(i > 0 ? ", ?" : "?");
        }
        
        callString.append(")}");
        
        String callSql = callString.toString();
        LoggingUtils.logSql(context.getLogger(), callSql);
        
        try (CallableStatement stmt = connection.prepareCall(callSql)) {
            // Register input parameters
            int paramIndex = 1;
            if (inParams != null) {
                for (ProcedureParam param : inParams) { setParameter(stmt, paramIndex++, param);}
            }
            
            // Register output parameters
            Map<String, Integer> outParamIndices = new HashMap<>();
            if (outParams != null) {
                for (ProcedureParam param : outParams) {
                    outParamIndices.put(param.getName(), paramIndex);
                    registerOutParameter(stmt, paramIndex++, param);
                }
            }
            
            // Execute the procedure
            stmt.execute();
            
            // Retrieve output parameter values
            Map<String, Object> results = new HashMap<>();
            for (Map.Entry<String, Integer> entry : outParamIndices.entrySet()) {results.put(entry.getKey(), stmt.getObject(entry.getValue()));}
            
            return results;
        }
    }
    
    private void setParameter(CallableStatement stmt, int index, ProcedureParam param) throws SQLException {
        String type = param.getType().toUpperCase();
        String value = param.getValue();
        switch (type) {
            case "NUMBER":
            case "INTEGER":
                stmt.setInt(index, Integer.parseInt(value));
                break;
            case "DOUBLE":
                stmt.setDouble(index, Double.parseDouble(value));
                break;
            default:
                stmt.setObject(index, value);
        }
    }
    
    private void registerOutParameter(CallableStatement stmt, int index, ProcedureParam param) throws SQLException {
        // Map param type to JDBC Types constant
        int sqlType = getJdbcType(param.getType());
        stmt.registerOutParameter(index, sqlType);
    }
    
    private int getJdbcType(String type) {
        // Map Java/SQL type names to JDBC Types constants
        return switch (type.toUpperCase()) {
            case "STRING", "VARCHAR", "VARCHAR2" -> Types.VARCHAR;
            case "INTEGER", "INT" -> Types.INTEGER;
            case "DOUBLE", "NUMBER" -> Types.DOUBLE;
            case "DATE" -> Types.DATE;
            case "TIMESTAMP" -> Types.TIMESTAMP;
            case "BOOLEAN" -> Types.BOOLEAN;
            default -> Types.OTHER;
        };
    }
    
    @Override
    public DatabaseContext getContext() {
        return context;
    }
}
