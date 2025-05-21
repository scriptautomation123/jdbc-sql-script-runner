package com.example.shelldemo.sqlhandling;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the pipeline for processing ResultSets.
 * Combines a ResultProcessor with optional ResultSetStreamer for flexible handling.
 *
 * @param <T> The type of result produced by the processor
 */
public class ResultProcessingPipeline<T> {
    private static final Logger logger = LogManager.getLogger(ResultProcessingPipeline.class);
    private final ResultProcessor<T> processor;
    private final ResultSetStreamer streamer;
    private final int batchSize;
    
    /**
     * Creates a new ResultProcessingPipeline with a processor and streamer.
     *
     * @param processor The processor to convert ResultSets to type T
     * @param streamer The streamer to stream results (may be null)
     * @param batchSize The batch size for streaming
     */
    public ResultProcessingPipeline(ResultProcessor<T> processor, ResultSetStreamer streamer, int batchSize) {
        this.processor = processor;
        this.streamer = streamer;
        this.batchSize = batchSize;
    }
    
    /**
     * Creates a new ResultProcessingPipeline with a processor only.
     *
     * @param processor The processor to convert ResultSets to type T
     */
    public ResultProcessingPipeline(ResultProcessor<T> processor) {
        this(processor, null, 100);
    }
    
    /**
     * Processes a ResultSet and optionally streams it.
     *
     * @param rs The ResultSet to process
     * @return The processed result
     * @throws SQLException if a database access error occurs
     * @throws IOException if a streaming I/O error occurs
     */
    public T processAndStream(ResultSet rs) throws SQLException, IOException {
        try {
            // Clone the ResultSet if we need to both process and stream
            if (streamer != null) {
                // Some databases support ResultSet cloning
                if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
                    // Process first
                    T result = processor.process(rs);
                    
                    // Reset and stream
                    rs.beforeFirst();
                    streamer.stream(rs, batchSize);
                    
                    return result;
                } else {
                    // Can't clone, so stream first to console/log
                    streamer.stream(rs, batchSize);
                    
                    // Can't process after streaming in forward-only mode
                    logger.warn("ResultSet is forward-only, can't process after streaming");
                    // Return a default/empty result
                    return null;
                }
            } else {
                // Just process, no streaming
                return processor.process(rs);
            }
        } catch (SQLException e) {
            logger.error("Error processing ResultSet: {}", e.getMessage(), e);
            throw e;
        }
    }
}
