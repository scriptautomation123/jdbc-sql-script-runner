package com.example.shelldemo.sqlhandling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.util.Map;


public class ConsoleOutputHandler implements ResultSetStreamer {
    private static final Logger logger = LogManager.getLogger(ConsoleOutputHandler.class);
    private final ResultSetProcessor processor;
    
    public ConsoleOutputHandler() {
        this.processor = new ResultSetProcessor();
    }
    
    @Override
    public void stream(ResultSet rs, int batchSize) throws SQLException, IOException {
        BatchProcessor<Map<String, Object>> batchProcessor = new BatchProcessor<>(batchSize,
            batch -> batch.forEach(row -> logger.info("Row: {}", row)));
        
        for (Map<String, Object> row : processor.processResultSet(rs)) {
            batchProcessor.add(row);
        }
        batchProcessor.flush();
    }
}