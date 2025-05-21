package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for processing ResultSet objects.
 * Provides a generic mechanism for converting ResultSets to various types.
 *
 * @param <T> The type of result produced by this processor
 */
public interface ResultProcessor<T> {
    
    /**
     * Processes a ResultSet and returns a result of type T.
     * The ResultSet cursor position should be considered consumed after calling this method.
     *
     * @param rs The ResultSet to process
     * @return The processed result
     * @throws SQLException if a database access error occurs
     */
    T process(ResultSet rs) throws SQLException;
}
