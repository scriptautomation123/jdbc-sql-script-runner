package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to map ResultSet rows to Map objects.
 */
public class ResultSetMapper {
    
    /**
     * Maps the current row of a ResultSet to a Map.
     * Note: This method does not call rs.next() - it maps the current row.
     * 
     * @param rs ResultSet positioned on the row to map
     * @return Map containing column name/value pairs
     * @throws SQLException if database access error occurs
     */
    public Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        Map<String, Object> row = new LinkedHashMap<>();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            if (columnName == null || columnName.isEmpty()) {
                columnName = metaData.getColumnName(i);
            }
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }
        
        return row;
    }
}
