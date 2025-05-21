package com.example.shelldemo.sqlhandling;

public class DatabaseConfig {
    private String dbType;
    private String host;
    private int port;
    private String username;
    private String password;
    private String database;
    private String connectionType;

    // Getters and setters
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    public void setServiceName(String serviceName) {
        this.database = serviceName;  // Assuming 'database' field exists
    }
}