package com.example.shelldemo.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.shelldemo.config.ConfigHolderAdapter;
import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;

@ExtendWith(NoStackTraceWatcher.class)
@DisplayName("Oracle Vendor Integration Tests")
class OracleVendor2Test extends BaseDbTest {
    
    private OracleVendor vendor;
    private ConfigurationService configService;
    
    private static boolean isVendorEnabled(String vendor) {
        String prop = System.getProperty("db.vendor");
        String env = System.getenv("DB_VENDOR");
        if (prop != null) return prop.equalsIgnoreCase(vendor);
        if (env != null) return env.equalsIgnoreCase(vendor);
        return true;
    }
    
    @BeforeEach
    void setUp() {
        if (!isVendorEnabled("oracle")) org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping non-Oracle test");
        configService = new ConfigHolderAdapter();
        vendor = new OracleVendor(configService);
    }
    
    @Test
    @DisplayName("Should build correct connection URL for thin service")
    void testBuildConnectionUrlThinService() {
        String url = vendor.buildConnectionUrl("localhost", 1521, "XE", "thin-service");
        assertTrue(url.startsWith("jdbc:oracle:thin:@//"));
        assertTrue(url.contains("localhost:1521/XE"));
    }
    
    @Test
    @DisplayName("Should build correct connection URL for thin SID")
    void testBuildConnectionUrlThinSid() {
        String url = vendor.buildConnectionUrl("localhost", 1521, "XE", "thin-sid");
        assertEquals("jdbc:oracle:thin:@localhost:1521:XE", url);
    }
    
    @Test
    @DisplayName("Should build correct connection URL for thin-ldap")
    void testBuildConnectionUrlThinLdap() {
        String url = vendor.buildConnectionUrl("localhost", 389, "XE", "thin-ldap");
        assertTrue(url.startsWith("jdbc:oracle:thin:@ldap://"));
        assertTrue(url.contains("localhost:389"));
    }
    
    @Test
    @DisplayName("Should get correct default port")
    void testGetDefaultPort() {
        assertEquals(1521, vendor.getDefaultPort());
    }
    
    @Test
    @DisplayName("Should get correct driver class name")
    void testGetDriverClassName() {
        assertEquals("oracle.jdbc.OracleDriver", vendor.getDriverClassName());
    }
    
    @Test
    @DisplayName("Should get default connection properties")
    void testGetDefaultConnectionProperties() {
        Properties props = vendor.getDefaultConnectionProperties();
        assertNotNull(props);
        assertEquals("false", props.getProperty("oracle.jdbc.fanEnabled"));
        assertEquals("20", props.getProperty("oracle.jdbc.implicitStatementCacheSize"));
        assertEquals("100000", props.getProperty("oracle.jdbc.maxCachedBufferSize"));
        assertEquals("100", props.getProperty("defaultRowPrefetch"));
    }
    
    @Test
    @DisplayName("Should detect PL/SQL blocks correctly")
    void testIsPLSQL() {
        assertTrue(vendor.isPLSQL("BEGIN\n  NULL;\nEND;"));
        assertTrue(vendor.isPLSQL("CREATE OR REPLACE PROCEDURE test_proc AS\nBEGIN\n  NULL;\nEND;"));
        assertTrue(vendor.isPLSQL("DECLARE\n  v_var NUMBER;\nBEGIN\n  NULL;\nEND;"));
        assertFalse(vendor.isPLSQL("SELECT * FROM dual"));
        assertFalse(vendor.isPLSQL("INSERT INTO table VALUES (1)"));
    }
    
    @Test
    @DisplayName("Should get correct explain plan SQL")
    void testGetExplainPlanSql() {
        String sql = "SELECT * FROM employees";
        String explainSql = vendor.getExplainPlanSql(sql);
        assertEquals("EXPLAIN PLAN FOR " + sql, explainSql);
    }
    
    @Test
    @DisplayName("Should get configuration service")
    void testGetConfigService() {
        assertEquals(configService, vendor.getConfigService());
    }
}
