package com.example.shelldemo;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.net.URL;
import java.net.URISyntaxException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

import com.example.shelldemo.testutil.BaseDbTest;
import com.example.shelldemo.testutil.NoStackTraceWatcher;
import com.example.shelldemo.config.ConfigurationService;
import com.example.shelldemo.config.ConfigHolderAdapter;

@DisplayName("UnifiedDatabaseRunner Integration Tests")
@ExtendWith(NoStackTraceWatcher.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UnifiedDatabaseRunnerTest extends BaseDbTest {

    private ConfigurationService configService;

    @BeforeEach
    @Override
    protected void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        configService = new ConfigHolderAdapter();
        try {
            ensureTestTableExists();
        } catch (Exception e) {
            fail("Failed to set up test table: " + e.getMessage(), e);
        }
    }

    private void ensureTestTableExists() throws Exception {
        String dropTable = "BEGIN EXECUTE IMMEDIATE 'DROP TABLE test_table'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;";
        String createTable = "CREATE TABLE test_table (id NUMBER PRIMARY KEY, name VARCHAR2(50))";
        Path dropFile = createSqlFile("drop_test_table.sql", dropTable);
        Path createFile = createSqlFile("create_test_table.sql", createTable);
        executeCommand(dropFile);
        executeCommand(createFile);
    }

    private static boolean isVendorEnabled(String vendor) {
        String prop = System.getProperty("db.vendor");
        String env = System.getenv("DB_VENDOR");
        if (prop != null) return prop.equalsIgnoreCase(vendor);
        if (env != null) return env.equalsIgnoreCase(vendor);
        return true;
    }

    static Stream<Arguments> vendorAndConnectionProvider() {
        return Stream.of(
            Arguments.of(
                "oracle",
                ORACLE_HOST, ORACLE_PORT, ORACLE_DATABASE, ORACLE_USERNAME, ORACLE_PASSWORD,
                "SELECT 1 FROM DUAL",
                "1"
            ),
            Arguments.of(
                "postgresql",
                "localhost", 5432, "testdb", "postgres", "postgres",
                "SELECT 1",
                "1"
            )
        ).filter(args -> isVendorEnabled((String) args.get()[0]));
    }

    @ParameterizedTest
    @Order(1)
    @MethodSource("vendorAndConnectionProvider")
    @DisplayName("Should verify database connection for Oracle and PostgreSQL")
    void testDatabaseConnection(
        String dbType, String host, int port, String database, String username, String password,
        String sql, String expectedOutput
    ) throws Exception {
        // Set up config for the current vendor
        configService.setRuntimeProperty("database.type", dbType);
        configService.setRuntimeProperty("database.host", host);
        configService.setRuntimeProperty("database.port", String.valueOf(port));
        configService.setRuntimeProperty("database.name", database);
        configService.setRuntimeProperty("database.username", username);
        configService.setRuntimeProperty("database.password", password);

        // Now do any vendor-specific setup
        ensureTestTableExists();

        Path sqlFile = createSqlFile("connection_test.sql", sql);
        String[] args = {
            sqlFile.toString(),
            "-t", dbType,
            "--connection-type", dbType.equals("oracle") ? "thin" : "default",
            "-H", host,
            "-P", String.valueOf(port),
            "-u", username,
            "-p", password,
            "-d", database,
            "--stop-on-error",
            "--print-statements"
        };
        ExecutionResult result = executeCommandWithArgs(args);
        assertSuccessfulExecution(result, output -> 
            output.contains(expectedOutput)
        );
    }

    @Test
    @Order(2)
    @DisplayName("Should handle invalid SQL gracefully")
    void testInvalidSql() throws Exception {
        Path sqlFile = createSqlFile("invalid.sql", "SELECT * FROM nonexistent_table");
        ExecutionResult result = executeCommand(sqlFile);
        assertNotEquals(0, result.exitCode(), "Should fail with non-zero exit code");
    }

    @Test
    @Order(3)
    @DisplayName("Should execute complex query with joins")
    void testComplexQuery() throws Exception {
        String complexQuery = "SELECT id, name FROM test_table WHERE id = 1";
        Path sqlFile = createSqlFile("hr_complex_query.sql", complexQuery);
        ExecutionResult result = executeCommand(sqlFile);
        assertSuccessfulExecution(result, output -> 
            output.toUpperCase().contains("ID") && 
            output.toUpperCase().contains("NAME")
        );
    }

    @Test
    @Order(4)
    @DisplayName("Should execute multiple SQL statements")
    void testMultipleStatements() throws Exception {
        String multipleQueries = String.join("\n",
            "SELECT employee_id, first_name, last_name FROM employees WHERE rownum <= 3;",
            "SELECT department_id, department_name FROM departments WHERE rownum <= 3;"
        );
        Path sqlFile = createSqlFile("test_script.sql", multipleQueries);
        ExecutionResult result = executeCommand(sqlFile);
        assertSuccessfulExecution(result, output -> 
            output.contains("EMPLOYEE_ID") && 
            output.contains("DEPARTMENT_ID")
        );
    }

    @Test
    @Order(5)
    @DisplayName("Should execute DDL script")
    void testDdlScript() throws URISyntaxException {
        Path sqlScriptPath = tempDir.resolve("test_ddl.sql");
        URL resourceUrl = getClass().getClassLoader().getResource("sql/create_employee_info_proc.sql");
        if (resourceUrl == null) {
            fail("Test SQL file not found in classpath");
        }
        
        Path sourcePath = Path.of(resourceUrl.toURI());
        copyAndVerifyFile(sourcePath, sqlScriptPath);
        ExecutionResult result = executeCommand(sqlScriptPath);
        
        assertSuccessfulExecution(result, output -> 
            output.contains("Successfully parsed 3 SQL statements") &&
            output.contains("Statement affected") &&
            output.contains("Database connection closed successfully")
        );
    }

    @Test
    @Order(6)
    @DisplayName("Should execute DML script transactionally when requested")
    void testDmlScriptTransactional() throws Exception {
        String dmlScript = String.join("\n",
            "INSERT INTO test_table (id, name) VALUES (1, 'A');",
            "INSERT INTO test_table (id, name) VALUES (2, 'B');"
        );
        Path sqlFile = createSqlFile("dml_script.sql", dmlScript);

        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        args = Stream.concat(
            Stream.of(args),
            Stream.of("--transactional")
        ).toArray(String[]::new);

        ExecutionResult result = executeCommandWithArgs(args);
        assertSuccessfulExecution(result, output ->
            output.contains("Executing DML statements in a transaction")
        );
    }

    @Test
    @Order(7)
    @DisplayName("Should execute DML script non-transactionally by default")
    void testDmlScriptNonTransactional() throws Exception {
        String dmlScript = String.join("\n",
            "INSERT INTO test_table (id, name) VALUES (3, 'C');",
            "INSERT INTO test_table (id, name) VALUES (4, 'D');"
        );
        Path sqlFile = createSqlFile("dml_script2.sql", dmlScript);
        ExecutionResult result = executeCommand(sqlFile);
        
        assertSuccessfulExecution(result, output ->
            output.contains("Executing DML statements non-transactionally")
        );
    }
}


