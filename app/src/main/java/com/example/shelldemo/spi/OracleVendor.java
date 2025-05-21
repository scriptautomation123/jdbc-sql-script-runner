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
 * Enhanced Oracle database vendor implementation.
 * Provides Oracle-specific behavior for database operations.
 */
public final class OracleVendor implements DatabaseVendor {
    private static final Logger logger = LogManager.getLogger(OracleVendor.class);
    
    // Regex pattern to detect PL/SQL blocks
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
        "^\\s*(?:DECLARE|BEGIN|CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:FUNCTION|PROCEDURE|PACKAGE|TRIGGER))",
        Pattern.CASE_INSENSITIVE
    );
    
    private final ConfigurationService configService;
    
    public OracleVendor(ConfigurationService configService) {
        this.configService = configService;
    }
    
    public OracleVendor() {
        this(new com.example.shelldemo.config.ConfigHolderAdapter());
    }
    
    @Override
    public String getVendorName() {
        return "oracle";
    }

    @SuppressWarnings("unchecked")
    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        if (connectionType != null && "thin-ldap".equalsIgnoreCase(connectionType)) {
            String template = configService.getJdbcTemplate("oracle", "ldap");
            var dbConfig = configService.getDatabaseConfig("oracle");
            var ldapConfig = (java.util.Map<String, Object>) dbConfig.get("ldap");
            String context = ldapConfig != null ? (String) ldapConfig.get("context") : "";
            return "jdbc:oracle:thin:@" + String.format(template, host, port > 0 ? port : 389, database, context);
        } else if (connectionType != null && "thin-service".equalsIgnoreCase(connectionType)) {
            String template = configService.getJdbcTemplate("oracle", "thin");
            return String.format(template, host, port > 0 ? port : getDefaultPort(), database);
        } else if (connectionType != null && "thin-sid".equalsIgnoreCase(connectionType)) {
            return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port > 0 ? port : getDefaultPort(), database);
        } else {
            String template = configService.getJdbcTemplate("oracle", "thin");
            return String.format(template, host, port > 0 ? port : getDefaultPort(), database);
        }
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("oracle.jdbc.fanEnabled", "false");
        props.setProperty("oracle.jdbc.implicitStatementCacheSize", "20");
        props.setProperty("oracle.jdbc.maxCachedBufferSize", "100000");
        props.setProperty("defaultRowPrefetch", "100");
        return props;
    }
    
    @Override
    public void initializeConnection(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Set session parameters
            stmt.execute("ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD'");
            stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF'");
            logger.debug("Oracle connection initialized with preferred NLS settings");
        } catch (SQLException e) {
            logger.warn("Failed to initialize Oracle connection settings", e);
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
        return 1521;
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }
    
    @Override
    public String getExplainPlanSql(String sql) {
        return "EXPLAIN PLAN FOR " + sql;
    }

    @Override
    public ConfigurationService getConfigService() {
        return configService;
    }
}
