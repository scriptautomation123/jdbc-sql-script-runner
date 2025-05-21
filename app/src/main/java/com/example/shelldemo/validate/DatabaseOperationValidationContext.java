package com.example.shelldemo.validate;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseOperationValidationContext {
    private final Connection connection;
    private final boolean showExplainPlan;
    private int statementCount;
    private String currentUsername;

    public DatabaseOperationValidationContext(Connection connection, boolean showExplainPlan) 
            throws SQLException {
        this.connection = connection;
        this.showExplainPlan = showExplainPlan;
        this.currentUsername = connection.getMetaData().getUserName();
        this.statementCount = 0;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isShowExplainPlan() {
        return showExplainPlan;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public int incrementAndGetCount() {
        return ++statementCount;
    }

    public int getStatementCount() {
        return statementCount;
    }
}