package com.example.shelldemo.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.config.ConfigurationService;

/**
 * Enhanced MySQL database vendor implementation.
 * Provides MySQL-specific behavior for database operations.
 */
public final class MySqlVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(MySqlVendor.class);
    
    // Pattern to detect MySQL procedural language blocks
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*(?:CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:PROCEDURE|FUNCTION|TRIGGER|EVENT))",
        Pattern.CASE_INSENSITIVE
    );
    
    private final ConfigurationService configService;
    
    public MySqlVendor(ConfigurationService configService) {
        this.configService = configService;
    }

    public MySqlVendor() {
        this(new com.example.shelldemo.config.ConfigHolderAdapter());
    }
    
    @Override
    public String getVendorName() {
        return "mysql";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        String template = configService.getJdbcTemplate("mysql", "default");
        String url = String.format(template, host, port > 0 ? port : getDefaultPort(), database);
        if (connectionType != null && !connectionType.isEmpty()) {
            url += "&" + connectionType;
        }
        return url;
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.putAll(configService.getDatabaseProperties("mysql"));
        return props;
    }
    
    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters for consistent behavior
            stmt.execute("SET time_zone = '+00:00'");
            stmt.execute("SET NAMES utf8mb4");
            stmt.execute("SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'");
            logger.debug("MySQL connection initialized with UTC timezone and strict SQL mode");
        } catch (SQLException e) {
            logger.warn("Failed to initialize MySQL connection settings", e);
        }
    }

    @Override
    public boolean isPLSQL(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        return PLSQL_PATTERN.matcher(sql.trim()).find();
    }
    
    @Override
    public int getDefaultPort() {
        return 3306;
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
    
    @Override
    public String getExplainPlanSql(String sql) {
        return "EXPLAIN " + sql;
    }
    
    @Override
    public String getValidationQuery() {
        return "SELECT 1";
    }

    @Override
    public ConfigurationService getConfigService() {
        return configService;
    }
}
