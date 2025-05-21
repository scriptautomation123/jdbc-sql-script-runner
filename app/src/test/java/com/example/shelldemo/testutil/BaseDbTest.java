package com.example.shelldemo.testutil;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.io.*;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.io.TempDir;
import org.apache.logging.log4j.Level;

import com.example.shelldemo.UnifiedDatabaseRunner;
import com.example.shelldemo.config.ConfigurationHolder;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public abstract class BaseDbTest {
    protected static final Logger logger = LogManager.getLogger(BaseDbTest.class);
    protected static final String SEPARATOR = "========================================";
    
    protected static final String ORACLE_HOST = "localhost";
    protected static final int ORACLE_PORT = 1521;
    protected static final String ORACLE_DATABASE = "FREEPDB1";
    protected static final String ORACLE_USERNAME = "HR";
    protected static final String ORACLE_PASSWORD = "HR";

    @TempDir
    protected Path tempDir;
    
    protected PrintStream originalOut;
    protected PrintStream originalErr;
    protected ByteArrayOutputStream outputStream;
    protected ByteArrayOutputStream logOutput;
    protected Appender testAppender;
    protected UnifiedDatabaseRunner runner;


    @BeforeAll
    static void setupLogging() {
        // Remove manual level setting, let log4j2-test.xml handle it
        System.setProperty("log4j2.formatMsgNoLookups", "true");
        System.setProperty("log4j.skipJansi", "true");
        ConfigurationHolder.getInstance();
    }
    
    @BeforeEach
    protected void setUp(TestInfo testInfo) {
        initializeStreams();
        initializeLogCapture();
        logTestStart(testInfo);
        runner = new UnifiedDatabaseRunner();
        
        // Suppress error logging for testInvalidSql
        if (testInfo.getDisplayName().contains("handle invalid SQL gracefully")) {
            org.apache.logging.log4j.core.config.Configurator.setLevel(
                "com.example.shelldemo", Level.OFF);
        }
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.setOut(originalOut);
        System.setErr(originalErr);
        removeLogCapture();
        
        // Restore logging level
        org.apache.logging.log4j.core.config.Configurator.setLevel(
            "com.example.shelldemo", Level.INFO);
    }

    protected void initializeStreams() {
        assertDoesNotThrow(() -> {
            outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            originalOut = System.out;
            originalErr = System.err;
            System.setOut(printStream);
            System.setErr(printStream);  // Use the same stream for both stdout and stderr
        }, "Stream setup should complete without errors");
    }

    protected void initializeLogCapture() {
        logOutput = new ByteArrayOutputStream();
        testAppender = new AbstractAppender("TestAppender", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                // Skip logging for testInvalidSql
                if (event.getThreadName().contains("testInvalidSql")) {
                    return;
                }
                
                if (event.getLevel().isMoreSpecificThan(Level.DEBUG)) {
                    String message = event.getMessage().getFormattedMessage();
                    if (!message.contains("at ") && !message.contains("Caused by:")) {
                        byte[] bytes = (message + "\n").getBytes();
                        logOutput.write(bytes, 0, bytes.length);
                    }
                }
            }
        };
        ((AbstractAppender) testAppender).start();
        
        org.apache.logging.log4j.core.Logger rootLogger = 
            (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        rootLogger.addAppender(testAppender);
    }

    protected void removeLogCapture() {
        if (testAppender != null) {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            rootLogger.removeAppender(testAppender);
        }
    }

    protected void logTestStart(TestInfo testInfo) {
        assertNotNull(testInfo.getDisplayName(), "Test name should be available");
        logger.info("Starting test: {}", testInfo.getDisplayName());
    }

    protected String[] createDefaultRunnerArgs(String target) {
        return new String[] {
            target,  // Positional parameter first
            "-t", "oracle",
            "--connection-type", "thin",
            "-H", ORACLE_HOST,
            "-P", String.valueOf(ORACLE_PORT),
            "-u", ORACLE_USERNAME,
            "-p", ORACLE_PASSWORD, 
            "-d", ORACLE_DATABASE,
            "--stop-on-error",
            "--print-statements"  // Remove "true" as it's a boolean flag
        };
    }

    protected Path createSqlFile(String fileName, String content) throws Exception {
        Path sqlFile = tempDir.resolve(fileName);
        Files.writeString(sqlFile, content);
        assertTrue(Files.exists(sqlFile), "SQL file should exist");
        assertTrue(Files.size(sqlFile) > 0, "SQL file should not be empty");
        return sqlFile;
    }

    protected void copyAndVerifyFile(Path source, Path target) {
        assertDoesNotThrow(() -> {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            assertTrue(Files.exists(target), "SQL script file should exist");
            assertTrue(Files.size(target) > 0, "SQL script file should not be empty");
        }, "File operations should complete without errors");
    }

    protected ExecutionResult executeCommand(Path sqlFile) {
        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        
        // Reset streams before execution
        outputStream.reset();
        logOutput.reset();
        
        // Redirect System.out/err to our stream
        PrintStream printStream = new PrintStream(outputStream, true);  // Added autoflush
        System.setOut(printStream);
        System.setErr(printStream);
        
        int exitCode = new CommandLine(runner).execute(args);
        
        // Ensure all output is flushed
        System.out.flush();
        System.err.flush();
        
        String output = outputStream.toString();
        String logOutputStr = logOutput.toString();
        
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        return new ExecutionResult(exitCode, output, logOutputStr);
    }

    protected void assertSuccessfulExecution(ExecutionResult result, java.util.function.Predicate<String> outputValidator) {
        if (result.exitCode() != 0) {
            fail(String.format("Command failed with exit code %d%nOutput: %s", 
                result.exitCode(), result.getCombinedOutput()));
        }
        
        String combinedOutput = result.getCombinedOutput();
        if (combinedOutput.isEmpty()) {
            fail("No output received from command");
        }
        
        if (!outputValidator.test(combinedOutput)) {
            fail(String.format("Expected output not found in:%n%s", combinedOutput));
        }
    }

    private String findErrors(String output) {
        StringBuilder errors = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.contains("ERROR") || line.contains("ORA-") || 
                line.contains("Exception") || line.contains("Error")) {
                errors.append(line).append("\n");
            }
        }
        return errors.toString();
    }

    protected void assertNoErrors(String output) {
        String errors = findErrors(output);
        assertTrue(errors.isEmpty(), "Found errors in output:\n" + errors);
    }

    public record ExecutionResult(int exitCode, String output, String logOutput) {
        public ExecutionResult(int exitCode, String output) {
            this(exitCode, output, "");
        }

        public String getCombinedOutput() {
            return output + logOutput;
        }
    }

    // Helper to allow custom args for CLI tests
    protected ExecutionResult executeCommandWithArgs(String[] args) {
        outputStream.reset();
        logOutput.reset();
        PrintStream printStream = new PrintStream(outputStream, true);
        System.setOut(printStream);
        System.setErr(printStream);
        int exitCode = new picocli.CommandLine(runner).execute(args);
        System.out.flush();
        System.err.flush();
        String output = outputStream.toString();
        String logOutputStr = logOutput.toString();
        System.setOut(originalOut);
        System.setErr(originalErr);
        return new ExecutionResult(exitCode, output, logOutputStr);
    }

}
