package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of ResultProcessor that converts a ResultSet to a List of Maps.
 * Each row becomes a Map with column names as keys and column values as values.
 */
public class MapResultProcessor implements ResultProcessor<List<Map<String, Object>>> {
    private static final Logger logger = LogManager.getLogger(MapResultProcessor.class);
    private final ResultSetMapper mapper;
    
    /**
     * Creates a new MapResultProcessor.
     */
    public MapResultProcessor() {
        this.mapper = new ResultSetMapper();
    }
    
    /**
     * Creates a new MapResultProcessor with a specific ResultSetMapper.
     *
     * @param mapper The ResultSetMapper to use
     */
    public MapResultProcessor(ResultSetMapper mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public List<Map<String, Object>> process(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        int rowCount = 0;
        
        while (rs.next()) {
            results.add(mapper.mapRow(rs));
            rowCount++;
        }
        
        logger.debug("Processed {} rows from ResultSet", rowCount);
        return results;
    }
}
