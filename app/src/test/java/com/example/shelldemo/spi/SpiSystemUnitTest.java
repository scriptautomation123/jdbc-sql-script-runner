package com.example.shelldemo.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for the SPI system with sealed interface hierarchy and service loading
 */
@ExtendWith(NoStackTraceWatcher.class)
class SpiSystemUnitTest extends BaseDbTest {

    @Test
    void testServiceDiscovery() {
        Collection<DatabaseVendor> vendors = VendorRegistry.getAllVendors().values();
        assertNotNull(vendors, "The vendor list should not be null");
        assertFalse(vendors.isEmpty(), "The vendor list should not be empty");
        
        // Should have our 4 standard vendors
        assertEquals(4, vendors.size(), "Should have exactly 4 vendor implementations");
        
        // Test vendor name retrieval
        Set<String> vendorNames = VendorRegistry.getAllVendors().keySet();
        assertTrue(vendorNames.contains("oracle"), "Oracle vendor should be registered");
        assertTrue(vendorNames.contains("postgresql"), "PostgreSQL vendor should be registered");
        assertTrue(vendorNames.contains("mysql"), "MySQL vendor should be registered");
        assertTrue(vendorNames.contains("sqlserver"), "SQL Server vendor should be registered");
    }
    
    @Test
    void testVendorLookup() {
        Optional<DatabaseVendor> oracleVendor = VendorRegistry.getVendor("oracle");
        assertTrue(oracleVendor.isPresent(), "Oracle vendor should be found");
        assertEquals("oracle", oracleVendor.get().getVendorName(), "Vendor name should match");
        
        Optional<DatabaseVendor> postgresVendor = VendorRegistry.getVendor("postgresql");
        assertTrue(postgresVendor.isPresent(), "PostgreSQL vendor should be found");
        assertEquals("postgresql", postgresVendor.get().getVendorName(), "Vendor name should match");
        
        Optional<DatabaseVendor> notFoundVendor = VendorRegistry.getVendor("nonexistent");
        assertFalse(notFoundVendor.isPresent(), "Nonexistent vendor should not be found");
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
    void testPatternMatchingVendorAgnostic(DatabaseVendor vendor) {
        assertNotNull(vendor.getVendorName());
        // Test Java 21 pattern matching with instanceof
        assertTrue(
            vendor instanceof OracleVendor ||
            vendor instanceof PostgreSqlVendor ||
            vendor instanceof MySqlVendor ||
            vendor instanceof SqlServerVendor,
            "Vendor should be a known implementation"
        );
        // Switch pattern matching for default port
        int expectedPort = switch(vendor) {
            case OracleVendor o -> 1521;
            case PostgreSqlVendor p -> 5432;
            case MySqlVendor m -> 3306;
            case SqlServerVendor s -> 1433;
            default -> throw new IllegalStateException("Unexpected vendor type: " + vendor);
        };
        assertEquals(vendor.getDefaultPort(), expectedPort, "Default port should match vendor");
    }
    
    @Test
    void testExhaustivePatternMatchingWithSealed() {
        Optional<DatabaseVendor> vendor = VendorRegistry.getVendor("postgresql");
        assertTrue(vendor.isPresent(), "PostgreSQL vendor should be found");
        
        // This switch must handle all possible subtypes due to the sealed interface
        String vendorCategory = switch(vendor.get()) {
            case OracleVendor o -> "Commercial Oracle Database";
            case PostgreSqlVendor p -> "Open Source PostgreSQL Database";
            case MySqlVendor m -> "Open Source MySQL Database";
            case SqlServerVendor s -> "Commercial SQL Server Database";
            default -> throw new IllegalStateException("Unexpected vendor type: " + vendor.get());
        };
        
        assertEquals("Open Source PostgreSQL Database", vendorCategory);
    }
}
