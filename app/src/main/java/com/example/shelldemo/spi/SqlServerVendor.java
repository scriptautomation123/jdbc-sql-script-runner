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
 * Enhanced SQL Server database vendor implementation.
 * Provides SQL Server-specific behavior for database operations.
 */
public final class SqlServerVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(SqlServerVendor.class);
    
    // Pattern to detect T-SQL procedural blocks
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*(?:CREATE\\s+(?:OR\\s+ALTER\\s+)?(?:PROCEDURE|FUNCTION|TRIGGER|VIEW))",
        Pattern.CASE_INSENSITIVE
    );
    
    private final ConfigurationService configService;
    
    public SqlServerVendor(ConfigurationService configService) {
        this.configService = configService;
    }
    
    public SqlServerVendor() {
        this(new com.example.shelldemo.config.ConfigHolderAdapter());
    }
    
    @Override
    public String getVendorName() {
        return "sqlserver";
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        String template = configService.getJdbcTemplate("sqlserver", "default");
        String url = String.format(template, host, port > 0 ? port : getDefaultPort(), database);
        url += ";encrypt=false;trustServerCertificate=true";
        if (connectionType != null && !connectionType.isEmpty()) {
            url += ";" + connectionType;
        }
        return url;
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.putAll(configService.getDatabaseProperties("sqlserver"));
        return props;
    }
    
    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters for consistent behavior
            stmt.execute("SET LANGUAGE us_english");
            stmt.execute("SET DATEFORMAT ymd");
            stmt.execute("SET ARITHABORT ON");
            logger.debug("SQL Server connection initialized with consistent date formatting");
        } catch (SQLException e) {
            logger.warn("Failed to initialize SQL Server connection settings", e);
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
        return 1433;
    }

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }
    
    @Override
    public String getExplainPlanSql(String sql) {
        return "SET SHOWPLAN_ALL ON; " + sql + "; SET SHOWPLAN_ALL OFF;";
    }
    
    @Override
    public String getValidationQuery() {
        return "SELECT 1 as test";
    }

    @Override
    public ConfigurationService getConfigService() {
        return configService;
    }
}
