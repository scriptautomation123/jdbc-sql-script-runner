package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Processor for JDBC ResultSet objects.
 * Handles conversion from ResultSets to more convenient data structures.
 */
public class ResultSetProcessor {
    private final ResultSetMapper mapper;

    public ResultSetProcessor() {
        this.mapper = new ResultSetMapper();
    }
    
    /**
     * Exception thrown when ResultSet processing fails.
     */
    public static class ResultSetProcessingException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public ResultSetProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Processes an entire ResultSet into a List of Maps.
     * 
     * @param rs ResultSet to process
     * @return List of maps containing column name/value pairs
     * @throws SQLException if database access error occurs
     */
    public List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()) {
            results.add(mapper.mapRow(rs));
        }
        return results;
    }

    /**
     * Processes a single row from a ResultSet.
     * Note: This method does not call rs.next() - it processes the current row.
     * 
     * @param rs ResultSet positioned on the row to process
     * @return Map containing column name/value pairs
     * @throws SQLException if database access error occurs
     */
    public Map<String, Object> processRow(ResultSet rs) throws SQLException {
        return mapper.mapRow(rs);
    }

    /**
     * Process a ResultSet with a streaming approach using a ResultSetStreamer.
     * 
     * @param rs ResultSet to stream
     * @param streamer The streamer to handle the data
     * @param batchSize The size of batches to process
     * @throws SQLException if database access error occurs
     * @throws IOException if I/O error occurs during streaming
     */
    public void streamResultSet(ResultSet rs, ResultSetStreamer streamer, int batchSize) 
            throws SQLException, IOException {
        try {
            streamer.stream(rs, batchSize);
        } catch (SQLException | IOException e) {
            throw new ResultSetProcessingException("Error while streaming ResultSet", e);
        }
    }
}
