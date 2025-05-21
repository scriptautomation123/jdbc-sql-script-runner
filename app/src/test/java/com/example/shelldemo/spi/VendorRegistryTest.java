package com.example.shelldemo.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for the database vendor registry and vendor implementations.
 * To run only Oracle tests: mvn test -Ddb.vendor=oracle
 * To run only PostgreSQL tests: mvn test -Ddb.vendor=postgresql
 * To run only MySQL tests: mvn test -Ddb.vendor=mysql
 * To run only SQL Server tests: mvn test -Ddb.vendor=sqlserver
 * To run all: no property needed
 */
@ExtendWith(NoStackTraceWatcher.class)
class VendorRegistryTest extends BaseDbTest {

    @Test
    void testAllVendorsAreLoaded() {
        // Test that all expected vendors are loaded
        var vendors = VendorRegistry.getAllVendors();
        
        // Should have 4 vendors
        assertEquals(4, vendors.size(), "Should have 4 vendor implementations");
        
        // Get all vendor names
        Set<String> vendorNames = vendors.keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        // Verify all expected vendors are present
        assertTrue(vendorNames.contains("oracle"), "Oracle vendor should be available");
        assertTrue(vendorNames.contains("postgresql"), "PostgreSQL vendor should be available");
        assertTrue(vendorNames.contains("mysql"), "MySQL vendor should be available");
        assertTrue(vendorNames.contains("sqlserver"), "SQL Server vendor should be available");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"oracle", "postgresql", "mysql", "sqlserver", "ORACLE", "PostgreSQL", "MySQL", "SQLServer"})
    void testGetVendorIsCaseInsensitive(String vendorName) {
        // Verify we can get each vendor regardless of case
        Optional<DatabaseVendor> vendor = VendorRegistry.getVendor(vendorName);
        
        assertTrue(vendor.isPresent(), "Should find vendor: " + vendorName);
        assertEquals(vendorName.toLowerCase(), vendor.get().getVendorName().toLowerCase(), 
                "Vendor name should match regardless of case");
    }
    
    @Test
    void testGetVendorWithInvalidName() {
        // Test behavior with invalid vendor name
        Optional<DatabaseVendor> vendor = VendorRegistry.getVendor("invalid");
        
        assertFalse(vendor.isPresent(), "Should not find vendor with invalid name");
    }
    
    @Test
    void testGetVendorWithNullName() {
        // Test behavior with null vendor name
        Optional<DatabaseVendor> vendor = VendorRegistry.getVendor(null);
        
        assertFalse(vendor.isPresent(), "Should handle null vendor name gracefully");
    }

    private static boolean isVendorEnabled(String vendor) {
        String prop = System.getProperty("db.vendor");
        String env = System.getenv("DB_VENDOR");
        if (prop != null) return prop.equalsIgnoreCase(vendor);
        if (env != null) return env.equalsIgnoreCase(vendor);
        return true;
    }

    static Stream<Arguments> vendorProvider() {
        return VendorRegistry.getAllVendors().values().stream().map(Arguments::of)
            .filter(args -> {
                DatabaseVendor v = (DatabaseVendor) args.get()[0];
                return isVendorEnabled(v.getVendorName());
            });
    }

    @ParameterizedTest
    @MethodSource("vendorProvider")
    void testVendorNameMatchesRegistry(DatabaseVendor vendor) {
        assertNotNull(vendor.getVendorName());
        assertTrue(VendorRegistry.getAllVendors().containsKey(vendor.getVendorName().toLowerCase()));
    }
}
