package com.example.shelldemo.sqlhandling;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Arrays;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class BatchExecutor {
    private static final Logger logger = LogManager.getLogger(BatchExecutor.class);
    private final Connection connection;
    
    public BatchExecutor(Connection connection) {
        this.connection = connection;
    }
    
    public int executeBatch(List<String> statements, boolean printStatements) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (String sql : statements) {
                if (printStatements) {
                    logger.info("Adding to batch: {}", sql);
                }
                stmt.addBatch(sql);
            }
            
            int[] results = stmt.executeBatch();
            int totalAffected = Arrays.stream(results)
                .filter(r -> r != Statement.SUCCESS_NO_INFO)
                .sum();
            
            logger.debug("Batch execution completed. Total rows affected: {}", totalAffected);
            return statements.size();
        }
    }
}