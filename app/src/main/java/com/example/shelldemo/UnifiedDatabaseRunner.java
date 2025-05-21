package com.example.shelldemo;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.Scanner;
import java.io.Console;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.example.shelldemo.exception.DatabaseOperationException;
import com.example.shelldemo.exception.VaultOperationException;
import com.example.shelldemo.vault.VaultSecretFetcherBuilder;
import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.config.ConfigurationHolder;
import com.example.shelldemo.vault.exception.VaultException;
import com.example.shelldemo.validate.DatabaserOperationValidator;

import java.util.Arrays;

@Command(name = "db", mixinStandardHelpOptions = true, version = "1.0",description = "Unified Database CLI Tool")
public class UnifiedDatabaseRunner implements Callable<Integer> {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseRunner.class);
    
    @Option(names = {"-t", "--type"}, required = true,description = "Database type (oracle, sqlserver, postgresql, mysql)")
    private String dbType;

    @Option(
        names = {"--connection-type"},
        description = "Connection type for Oracle (thin, thin-ldap). Defaults to thin-ldap if not specified."
    )
    private String connectionType;

    @Option(names = {"-H", "--host"}, description = "Database host")
    private String host;

    @Option(names = {"-P", "--port"}, description = "Database port (defaults: oracle=1521, sqlserver=1433, postgresql=5432, mysql=3306)")
    private int port;

    @Option(names = {"-u", "--username"}, required = true, description = "Database username")
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database password")
    private String password;

    @Option(names = {"-d", "--database"}, required = true, description = "Database name")
    private String database;

    @Option(names = {"--stop-on-error"}, defaultValue = "true",description = "Stop execution on error")
    private boolean stopOnError;

    @Option(names = {"--auto-commit"}, defaultValue = "false",description = "Auto-commit mode")
    private boolean autoCommit;

    @Option(names = {"--print-statements"}, defaultValue = "false",description = "Print SQL statements")
    private boolean printStatements;

    @Parameters(index = "0", paramLabel = "TARGET", description = "SQL script file or stored procedure name", arity = "0..1")
    private String target;

    @Option(names = {"--function"}, description = "Execute as function")
    private boolean isFunction;

    @Option(names = {"--return-type"}, defaultValue = "NUMERIC",description = "Return type for functions")
    private String returnType;

    @Option(names = {"-i", "--input"}, description = "Input parameters (name:type:value,...)")
    private String inputParams;

    @Option(names = {"-o", "--output"}, description = "Output parameters (name:type,...)")
    private String outputParams;

    @Option(names = {"--io"}, description = "Input/Output parameters (name:type:value,...)")
    private String ioParams;

    @Option(names = {"--driver-path"}, description = "Path to JDBC driver JAR file")
    private String driverPath;

    @Option(names = {"--csv-output"}, description = "Output file for CSV format (if query results exist)")
    private String csvOutputFile;

    @Option(names = {"--pre-flight"}, description = "Validate statements without executing them")
    private boolean preFlight;

    @Option(names = {"--validate-script"}, description = "Show execution plan and validate syntax for each statement during pre-flight")
    private boolean showExplainPlan;

    @Option(names = {"--transactional"}, defaultValue = "false", description = "Execute DML statements in a transaction (default: false)")
    private boolean transactional;

    @Option(names = {"--show-connect-string"}, description = "Show the generated JDBC connection string and exit")
    private boolean showConnectString;

    @Option(names = {"--secret"}, description = "Fetch Oracle password from Vault using secret name (mutually exclusive with -p/--password)")
    private String vaultSecretId;

    @Option(names = {"--vault-url"}, description = "Vault base URL")
    private String vaultBaseUrl;

    @Option(names = {"--vault-role-id"}, description = "Vault role ID")
    private String vaultRoleId;

    @Option(names = {"--vault-ait"}, description = "Vault AIT")
    private String vaultAit;

    @Override
    public Integer call() throws DatabaseOperationException {
        logger.debug("Entering call()");
        setupDriverPathIfNeeded();
        Integer result = null;
        try {
            logger.info("Starting database operation - type: {}, target: {}", dbType, target);

            if (!validatePasswordOptions()) {
                return 2;
            }

            if (showConnectString) {
                return showConnectString();
            }

            if (!validateTarget()) {
                return 2;
            }

            if (!setupPassword()) {
                return 2;
            }

            if (!validateOracleConnection()) {
                return 2;
            }
            logger.info("Starting database operation - type: {}, target: {}", dbType, target);
            result = runDatabaseOperation();
        } catch (DatabaseOperationException e) {
            logger.error("Database operation failed: {}", e.getMessage(), e);
            result = 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid operation parameters: {}", e.getMessage(), e);
            result = 2;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            result = 3;
        } finally {
            logger.debug("Exiting call() with result: {}", result);
        }
        return result;
    }

    private boolean validatePasswordOptions() {
        if (vaultSecretId != null && password != null && !password.trim().isEmpty()) {
            logger.error("--secret and -p/--password are mutually exclusive. Please specify only one.");
            return false;
        }
        return true;
    }

    private boolean validateTarget() {
        if (target == null || target.trim().isEmpty()) {
            logger.error("Target file or procedure name is required");
            return false;
        }
        return true;
    }

    private boolean setupPassword() {
        logger.debug("Entering setupPassword()");


        // 1. If password is provided, use it
        if (password != null && !password.trim().isEmpty()) {
            logger.debug("Password provided via command line");
            return true;
        }

        // 2. If vaultSecretId is provided on the command line
        if (vaultSecretId != null && !vaultSecretId.trim().isEmpty()) {
            if (vaultBaseUrl == null || vaultRoleId == null || vaultAit == null ||vaultBaseUrl.trim().isEmpty() || vaultRoleId.trim().isEmpty() || vaultAit.trim().isEmpty()) {
                logger.error("If --secret is provided, --vault-url, --vault-role-id, and --vault-ait must also be provided.");
                return false;
            }
            logger.debug("All Vault command-line parameters provided, fetching password from Vault");
            try {
                password = fetchPasswordFromVaultWithParams(vaultBaseUrl, vaultRoleId, vaultSecretId, vaultAit);
            } catch (VaultOperationException e) {
                logger.error("Failed to fetch password from Vault: {}", e.getMessage());
                return false;
            }
            logger.debug("Exiting setupPassword() with password fetched from Vault (command-line params)");
            return true;
        }

        // 3. If vault config is present in application.yaml
        var config = ConfigurationHolder.getInstance();
        var vaultConfig = config.getVaultConfig();
        
        // Check if vault entries are not null and not empty strings
        boolean vaultConfigured = vaultConfig != null
            && vaultConfig.getOrDefault("vault-base-url", "").toString().trim().length() > 0
            && vaultConfig.getOrDefault("vault-role-id", "").toString().trim().length() > 0
            && vaultConfig.getOrDefault("vault-secret-id", "").toString().trim().length() > 0
            && vaultConfig.getOrDefault("vault-ait", "").toString().trim().length() > 0;

        logger.debug("vaultConfig {}", vaultConfig.toString());

        if (vaultConfigured) {
            // Set vault fields from application.yaml if not set via command line
            if (vaultBaseUrl == null) vaultBaseUrl = (String) vaultConfig.get("vault-base-url");
            if (vaultRoleId == null) vaultRoleId = (String) vaultConfig.get("vault-role-id");
            if (vaultSecretId == null) vaultSecretId = (String) vaultConfig.get("vault-secret-id");
            if (vaultAit == null) vaultAit = (String) vaultConfig.get("vault-ait");

            logger.debug("Vault is configured in application.yaml, fetching password from Vault");
            try {
                password = fetchPasswordFromVault();
                if (password == null || password.trim().isEmpty()) {
                    logger.error("Vault returned empty password");
                    return false;
                }
                logger.debug("Successfully fetched password from Vault");
                return true;
            } catch (VaultOperationException e) {
                logger.error("Failed to fetch password from Vault: {}", e.getMessage());
                return false;  // Error out if vault is configured but fails
            }
        }

        // 4. Prompt for password only if vault is not configured
        logger.debug("Vault not configured, prompting user for password");
        password = promptForPassword();
        logger.debug("Exiting setupPassword() with password from prompt");
        return true;
    }

    private boolean validateOracleConnection() {
        if (!"oracle".equalsIgnoreCase(dbType)) {
            return true;
        }

        if (connectionType == null) {
            connectionType = "thin-ldap"; // Default for Oracle
        } else if (!connectionType.equalsIgnoreCase("thin") && 
                  !connectionType.equalsIgnoreCase("thin-ldap")) {
            logger.error("Invalid --connection-type: {}. Allowed values are 'thin' or 'thin-ldap'.", connectionType);
            return false;
        }

        if ("thin".equalsIgnoreCase(connectionType) && 
            (host == null || host.trim().isEmpty())) {
            logger.error("Host is required for connection type 'thin'");
            return false;
        }
        return true;
    }

    private int showConnectString() {
        logger.debug("Entering showConnectString()");
        ConnectionConfig connConfig = ConnectionConfig.builder()
            .dbType(dbType)
            .host(host)
            .port(port)
            .username(username)
            .password(password)
            .serviceName(database)
            .connectionType(connectionType)
            .build();
            
        String connectString = new DatabaseConnectionFactory().buildConnectionUrl(connConfig);
        logger.info(connectString);
        if ("thin".equalsIgnoreCase(connectionType) && (host == null || host.trim().isEmpty())) {
            logger.error("Host is required for connection type 'thin'");
            return 2;
        }
        logger.debug("Exiting showConnectString() with result: {}", 0);
        return 0;
    }

    private String promptForPassword() {
        logger.debug("Entering promptForPassword()");
        System.out.print("Enter database password: ");
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword();
            if (pwd != null) return new String(pwd);
        } else {
            try (Scanner scanner = new Scanner(System.in)) {
                return scanner.nextLine();
            }
        }
        logger.debug("Exiting promptForPassword()");
        return "";
    }

    private int runDatabaseOperation() {
        logger.debug("Entering runDatabaseOperation()");
        try (UnifiedDatabaseOperation operation = new UnifiedDatabaseOperationBuilder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .dbType(dbType)
                .serviceName(database)
                .connectionType(connectionType)
                .transactional(transactional)
                .build()
            ) {
            File scriptFile = new File(target);

            if (scriptFile.isDirectory()) {
                logger.error("Target '{}' is a directory, expected a file or procedure name", target);
                return 2;
            }

            if (!scriptFile.exists()) {
                if (target.contains("/") || target.contains("\\")) {
                    logger.error("File not found: {}", target);
                    return 2;
                }
                logger.debug("Executing as stored procedure: {}", target);
                operation.callStoredProcedure(target, null, null);
                return 0;
            }

            if (preFlight) {
                new DatabaserOperationValidator(dbType).validateScript(
                    operation.getContext().getConnection(),
                    scriptFile.getPath(),
                    showExplainPlan,
                    operation.getVendor()
                );
                return 0;
            }

            logger.debug("Executing as script file: {}", scriptFile.getAbsolutePath());
            operation.executeScript(scriptFile);
            return 0;
        } catch (DatabaseOperationException e) {
            logger.error("Database operation failed: {}", e.getMessage(), e);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid operation parameters: {}", e.getMessage(), e);
            return 2;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return 3;
        } finally {
            logger.debug("Exiting runDatabaseOperation() with result: {}", 0);
        }
    }

    private String fetchPasswordFromVault() throws VaultOperationException {
        // Try command line args first, fall back to config
       
        String baseUrl = vaultBaseUrl;
        String roleId = vaultRoleId;
        String secretId = vaultSecretId;
        String dbName = database;
        String ait = vaultAit;

        // If any required parameter is missing, try to get from config
        if (baseUrl == null || roleId == null || secretId == null || ait == null) {
            var config = ConfigurationHolder.getInstance();
            var vaultConfig = config.getVaultConfig();
            if (baseUrl == null) baseUrl = (String) vaultConfig.get("vault-base-url");
            if (roleId == null) roleId = (String) vaultConfig.get("vault-role-id");
            if (secretId == null) vaultSecretId = (String) vaultConfig.get("vault-secret-id");
            if (ait == null) ait = (String) vaultConfig.get("vault-ait");
        }

        // Validate all parameters are present
        if (baseUrl == null || dbName == null || roleId == null || secretId == null || ait == null) {
            throw new VaultOperationException("Missing required Vault configuration parameters", null, secretId);
        }

        try {
            return new VaultSecretFetcherBuilder()
                .build()
                .fetchOraclePassword(
                    baseUrl, roleId, secretId, dbName, ait, username
                );
        } catch (VaultException e) {
            throw new VaultOperationException("Failed to fetch password from Vault: " + e.getMessage(), e, secretId);
        }
    }

    // Helper for explicit command-line vault params
    private String fetchPasswordFromVaultWithParams(String baseUrl, String roleId, String secretId, String ait) throws VaultOperationException {
        String dbName = database;
        if (baseUrl == null || dbName == null || roleId == null || secretId == null || ait == null) {
            throw new VaultOperationException("Missing required Vault configuration parameters", null, secretId);
        }
        try {
            return new VaultSecretFetcherBuilder()
                .build()
                .fetchOraclePassword(
                    baseUrl, roleId, secretId, dbName, ait, username
                );
        } catch (VaultException e) {
            throw new VaultOperationException("Failed to fetch password from Vault: " + e.getMessage(), e, secretId);
        }
    }

    /**
     * Auto-select the correct JDBC driver if --driver-path is not provided, based on dbType.
     */
    private void setupDriverPathIfNeeded() {
        if (driverPath != null && !driverPath.trim().isEmpty()) {
            logger.debug("Driver path provided: {}", driverPath);
            return;
        }
        if (dbType == null || dbType.trim().isEmpty()) {
            logger.warn("Database type (-t/--type) is not specified; cannot auto-select driver.");
            return;
        }
        String driverDir = "drivers/" + dbType.toLowerCase();
        File dir = new File(driverDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                driverPath = jars[0].getAbsolutePath();
                logger.info("Auto-selected driver for {}: {}", dbType, driverPath);
            } else {
                logger.warn("No driver JAR found in {}", driverDir);
            }
        } else {
            logger.warn("Driver directory does not exist: {}", driverDir);
        }
    }

    public static void main(String[] args) {
        logger.debug("Entering main with args: {}", Arrays.toString(args));
        // Configure Log4j programmatically
        ConfigurationBuilder<BuiltConfiguration> log4jConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();
        
        // Create appenders
        log4jConfigBuilder.add(log4jConfigBuilder.newAppender("Console", "CONSOLE").addAttribute("target", "SYSTEM_OUT")
                                                 .add(log4jConfigBuilder.newLayout("PatternLayout")
                                                 .addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")));
        
        // Create root logger
        log4jConfigBuilder.add(log4jConfigBuilder.newRootLogger(org.apache.logging.log4j.Level.INFO)
                                                 .add(log4jConfigBuilder.newAppenderRef("Console")));
        // Configure async logging
        log4jConfigBuilder.add(log4jConfigBuilder.newAsyncLogger("com.example.shelldemo", org.apache.logging.log4j.Level.DEBUG)
                                                 .addAttribute("includeLocation", "true"));
        // Initialize Log4j
        Configurator.initialize(log4jConfigBuilder.build());
        
        logger.debug("Starting UnifiedDatabaseRunner...");
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        logger.debug("UnifiedDatabaseRunner completed with exit code: {}", exitCode);
        System.exit(exitCode);
        logger.debug("Exiting main");
    }
}
