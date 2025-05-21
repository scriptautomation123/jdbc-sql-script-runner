package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;

public interface ResultSetStreamer {
    void stream(ResultSet rs, int batchSize) throws SQLException, IOException;
} 