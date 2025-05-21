package com.example.shelldemo.spi;

import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(NoStackTraceWatcher.class)
public class DatabaseVendorTest extends BaseDbTest {

    private static boolean isVendorEnabled(String vendor) {
        String prop = System.getProperty("db.vendor");
        String env = System.getenv("DB_VENDOR");
        if (prop != null) return prop.equalsIgnoreCase(vendor);
        if (env != null) return env.equalsIgnoreCase(vendor);
        return true;
    }

    @Test
    void testPostgresValidationQuery() {
        if (!isVendorEnabled("postgresql")) return;
        DatabaseVendor vendor = new PostgreSqlVendor();
        assertEquals("SELECT 1", vendor.getValidationQuery());
    }

    @Test
    void testPostgresExplainPlanSql() {
        if (!isVendorEnabled("postgresql")) return;
        DatabaseVendor vendor = new PostgreSqlVendor();
        String sql = "SELECT * FROM employees";
        assertEquals("EXPLAIN (ANALYZE false, COSTS true, FORMAT TEXT) " + sql, vendor.getExplainPlanSql(sql));
    }

    @Test
    void testPostgresPLSQLDetection() {
        if (!isVendorEnabled("postgresql")) return;
        DatabaseVendor vendor = new PostgreSqlVendor();
        assertTrue(vendor.isPLSQL("DO $$ BEGIN RAISE NOTICE 'Hello'; END $$;"));
        assertFalse(vendor.isPLSQL("SELECT * FROM employees"));
    }
} 