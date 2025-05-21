package com.example.shelldemo.config;

/**
 * Immutable record for simplified database configuration.
 * Used in the refactored API for easier parameter passing.
 */
public record DatabaseConfig(
    String vendor,
    String host,
    int port,
    String database,
    String username,
    String password,
    String connectionType
) {
    /**
     * Creates a new configuration with default values.
     * 
     * @param vendor The database vendor (oracle, postgresql, mysql, sqlserver)
     * @param database The database name
     * @return A new configuration with default values
     */
    public static DatabaseConfig withDefaults(String vendor, String database) {
        return switch (vendor.toLowerCase()) {
            case "oracle" -> new DatabaseConfig(vendor, "localhost", 1521, database, "system", "oracle", null);
            case "postgresql" -> new DatabaseConfig(vendor, "localhost", 5432, database, "postgres", "postgres", null);
            case "mysql" -> new DatabaseConfig(vendor, "localhost", 3306, database, "root", "root", null);
            case "sqlserver" -> new DatabaseConfig(vendor, "localhost", 1433, database, "sa", "Password123!", null);
            default -> throw new IllegalArgumentException("Unsupported vendor: " + vendor);
        };
    }
}
