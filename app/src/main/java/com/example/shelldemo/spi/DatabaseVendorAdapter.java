package com.example.shelldemo.spi;

import java.sql.Connection;
import java.util.Properties;
import com.example.shelldemo.config.ConfigurationService;

/**
 * Adapter class to use DatabaseVendor2 where DatabaseVendor is expected.
 * This is a temporary solution for the transition period.
 */
public final class DatabaseVendorAdapter implements DatabaseVendor {
    private final DatabaseVendor adaptee;
    private final ConfigurationService configService;
    
    public DatabaseVendorAdapter(DatabaseVendor adaptee, ConfigurationService configService) {
        this.adaptee = adaptee;
        this.configService = configService;
    }

    @Override
    public String getVendorName() {
        return adaptee.getVendorName();
    }

    @Override
    public String buildConnectionUrl(String host, int port, String database, String connectionType) {
        return adaptee.buildConnectionUrl(host, port, database, connectionType);
    }

    @Override
    public Properties getDefaultConnectionProperties() {
        return adaptee.getDefaultConnectionProperties();
    }

    @Override
    public void initializeConnection(Connection connection) {
        adaptee.initializeConnection(connection);
    }

    @Override
    public boolean isPLSQL(String sql) {
        return adaptee.isPLSQL(sql);
    }

    @Override
    public int getDefaultPort() {
        return adaptee.getDefaultPort();
    }

    @Override
    public ConfigurationService getConfigService() {
        return configService;
    }
} 