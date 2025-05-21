package com.example.shelldemo.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.util.LoggingUtils;
import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.config.ConfigHolderAdapter;

/**
 * Utility class for loading and accessing database vendor implementations.
 * Uses Java's ServiceLoader to dynamically load vendor implementations.
 */
public final class VendorRegistry {
    private static final Logger logger = LogManager.getLogger(VendorRegistry.class);
    
    private static final Map<String, DatabaseVendor> vendors = new HashMap<>();
    
    private static ConfigurationService configService = new ConfigHolderAdapter();
    
    static {
        loadVendors();
    }
    
    private VendorRegistry() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Loads all available database vendor implementations.
     */
    private static void loadVendors() {
        LoggingUtils.debug(logger, "Loading database vendor implementations");
        
        ServiceLoader<DatabaseVendor> loader = ServiceLoader.load(DatabaseVendor.class);
        
        for (DatabaseVendor vendor : loader) {
            String vendorName = vendor.getVendorName().toLowerCase();
            // Re-instantiate known vendor types with configService
            DatabaseVendor instance;
            switch (vendorName) {
                case "oracle" -> instance = new com.example.shelldemo.spi.OracleVendor(configService);
                case "postgresql" -> instance = new com.example.shelldemo.spi.PostgreSqlVendor(configService);
                case "mysql" -> instance = new com.example.shelldemo.spi.MySqlVendor(configService);
                case "sqlserver" -> instance = new com.example.shelldemo.spi.SqlServerVendor(configService);
                default -> instance = new com.example.shelldemo.spi.DatabaseVendorAdapter(vendor, configService);
            }
            vendors.put(vendorName, instance);
            LoggingUtils.info(logger, "Loaded database vendor: {}", vendorName);
        }
        
        if (vendors.isEmpty()) {
            LoggingUtils.warn(logger, "No database vendor implementations found");
        }
    }
    
    /**
     * Gets a database vendor by name.
     *
     * @param vendorName The vendor name
     * @return An Optional containing the vendor, or empty if not found
     */
    public static Optional<DatabaseVendor> getVendor(String vendorName) {
        if (vendorName == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(vendors.get(vendorName.toLowerCase()));
    }
    
    /**
     * Gets all available database vendors.
     *
     * @return A map of vendor names to vendor implementations
     */
    public static Map<String, DatabaseVendor> getAllVendors() {
        return Map.copyOf(vendors);
    }
    
    /**
     * Reloads all vendor implementations.
     * This is primarily useful for testing.
     */
    public static void reloadVendors() {
        vendors.clear();
        loadVendors();
    }
    
    public static void setConfigService(ConfigurationService service) {
        configService = service;
        reloadVendors();
    }
}
