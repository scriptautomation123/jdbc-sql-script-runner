package com.example.shelldemo.sqlhandling;

import java.sql.Statement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementHandler {
    void handle(Statement stmt, String sql) throws SQLException;
}