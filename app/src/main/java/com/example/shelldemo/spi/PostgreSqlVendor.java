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
 * Enhanced PostgreSQL database vendor implementation.
 * Provides PostgreSQL-specific behavior for database operations.
 */
public final class PostgreSqlVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(PostgreSqlVendor.class);
    
    // Pattern to detect PostgreSQL procedural language blocks (PL/pgSQL)
    private static final Pattern PLPGSQL_PATTERN = Pattern.compile(
        "^\\s*(?:DO|CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:FUNCTION|PROCEDURE|TRIGGER))",
        Pattern.CASE_INSENSITIVE
    );
    
    private final ConfigurationService configService;
    
    public PostgreSqlVendor(ConfigurationService configService) {
        this.configService = configService;
    }
    
    public PostgreSqlVendor() {
        this(new com.example.shelldemo.config.ConfigHolderAdapter());
    }
    
    @Override
    public String getVendorName() {
        return "postgresql";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        String template = configService.getJdbcTemplate("postgresql", "default");
        String url = String.format(template, host, port > 0 ? port : getDefaultPort(), database);
        if (connectionType != null && !connectionType.isEmpty()) {
            url += "?" + connectionType;
        }
        return url;
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.putAll(configService.getDatabaseProperties("postgresql"));
        return props;
    }
    
    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters for consistent behavior
            stmt.execute("SET TIME ZONE 'UTC'");
            stmt.execute("SET datestyle TO 'ISO, YMD'");
            logger.debug("PostgreSQL connection initialized with UTC timezone and ISO date style");
        } catch (SQLException e) {
            logger.warn("Failed to initialize PostgreSQL connection settings", e);
        }
    }

    @Override
    public boolean isPLSQL(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        return PLPGSQL_PATTERN.matcher(sql.trim()).find();
    }
    
    @Override
    public int getDefaultPort() {
        return 5432;
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }
    
    @Override
    public String getExplainPlanSql(String sql) {
        return "EXPLAIN (ANALYZE false, COSTS true, FORMAT TEXT) " + sql;
    }

    @Override
    public ConfigurationService getConfigService() {
        return configService;
    }
}
