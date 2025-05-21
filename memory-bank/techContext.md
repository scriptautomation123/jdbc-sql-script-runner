# Technology Context
<!-- markdownlint-disable MD022 MD032 MD022 MD02 MD009 MD047 MD028 MD037 MD040-->
## Core Technologies

### Primary Language: Java

Java is the primary implementation language for the SQL Parser and Database Management tool:

- **Object-Oriented Approach**: Structured, maintainable code with clear class hierarchies
- **Strong Typing**: Type safety ensures reliable code execution
- **Cross-Platform**: JVM enables deployment across different operating systems
- **Rich Ecosystem**: Access to numerous libraries and tools

```java
// Example Java code from the SQL parser
public final class SqlScriptParser {
    private static final Logger logger = LogManager.getLogger(SqlScriptParser.class);

    private SqlScriptParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    public static Map<Integer, String> parseSqlFile(File scriptFile) throws SqlParseException {
        // Implementation details...
    }
}
```

### Runtime Target: JVM

The application runs on the Java Virtual Machine (JVM):

- **Performance**: Optimized execution with JIT compilation
- **Memory Management**: Automatic garbage collection
- **Platform Independence**: Write once, run anywhere capability
- **Scalability**: Handles large SQL scripts efficiently

## Development Environment

### Build System: Maven (Inferred)

- **Dependency Management**: Handles external libraries
- **Build Lifecycle**: Standard build phases and goals
- **Plugin Architecture**: Extensible build process
- **Consistent Structure**: Standard project organization

```xml
<!-- Inferred from project structure -->
<dependencies>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.x.x</version>
    </dependency>
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>4.x.x</version>
    </dependency>
    <!-- JDBC drivers and other dependencies -->
</dependencies>
```

### Testing Framework: JUnit (Inferred)

- **Unit Testing**: Test individual components
- **Mocking Support**: Isolate components for testing
- **Assertions**: Verify expected behavior
- **Test Runners**: Organize and execute tests

### Command Line Interface: Picocli

- **Annotation-Based**: Simple declaration of CLI options
- **Type Conversion**: Automatic conversion of arguments
- **Help Generation**: Automatic help text generation
- **Subcommands**: Hierarchical command structure
- **Supports --transactional flag for DML script execution**

```java
@Command(name = "db", mixinStandardHelpOptions = true, version = "1.0",
    description = "Unified Database CLI Tool")
public class UnifiedDatabaseRunner implements Callable<Integer> {
    @Option(names = {"-t", "--type"}, required = true,
        description = "Database type (oracle, sqlserver, postgresql, mysql)")
    private String dbType;
    @Option(names = {"--transactional"}, defaultValue = "false", description = "Execute DML statements in a transaction (default: false)")
    private boolean transactional;
}
```

## Dependencies

### Core Dependencies

| Dependency | Purpose | Usage |
|------------|---------|-------|
| Log4j | Logging | Comprehensive logging throughout the application |
| Picocli | Command-line parsing | CLI interface implementation |
| JDBC | Database connectivity | Connecting to various database systems |
| YAML | Configuration | Loading configuration settings |

### Development Dependencies

| Dependency | Purpose | Usage |
|------------|---------|-------|
| JUnit | Testing | Unit and integration testing |
| Mockito | Mocking | Isolating components for testing |
| Maven | Build | Project build and dependency management |

## Technical Architecture

### Component Structure

The application is organized into several key packages:

1. **parser**: SQL parsing and tokenization
   - **SqlScriptParser**: Parses SQL files into individual statements
   - **storedproc**: Handles stored procedure parsing

2. **connection**: Database connectivity
   - **DatabaseConnectionFactory**: Creates connections to different databases
   - **ConnectionConfig**: Configures connection parameters

3. **exception**: Error handling
   - Hierarchical exception structure (DatabaseException, ParserException, etc.)
   - Specific exception types for different failure modes

4. **validate**: Validation logic
   - **DatabaserOperationValidator**: Validates SQL operations
   - **StoredProcedureValidator**: Validates stored procedure syntax

5. **config**: Configuration management
   - **ConfigurationHolder**: Central configuration access
   - **YamlConfigReader**: Loads YAML configuration

6. **error**: Error handling utilities
   - **DatabaseErrorFormatter**: Formats database-specific errors

### Core Classes

1. **UnifiedDatabaseRunner**: Main entry point and CLI interface
2. **UnifiedDatabaseOperation**: Core database operations implementation
   - **Partitions DML and non-DML statements for script execution; DML can be executed transactionally via CLI flag**
3. **SqlScriptParser**: Parses SQL scripts into executable statements
4. **DatabaseConnectionFactory**: Creates appropriate database connections

## Database Support

### Supported Databases

The application supports multiple database systems:

| Database | Support Level | Notes |
|----------|---------------|-------|
| Oracle | Full | Includes LDAP and thin connection types |
| SQL Server | Full | Standard JDBC connectivity |
| PostgreSQL | Full | Standard JDBC connectivity |
| MySQL | Full | Standard JDBC connectivity |

### SQL Dialect Features

The parser recognizes and handles various SQL dialect features:

- **PL/SQL blocks**: Correctly parses BEGIN/END blocks
- **Statement delimiters**: Handles semicolons and forward slashes
- **Quoted identifiers**: Handles dialect-specific quoting rules
- **Comments**: Supports single-line (--) and multi-line (/* */) comments

## Parsing Implementation

### Parsing Strategy

The SQL parser uses a character-by-character approach:

```
Input SQL File → Character Stream → Tokenization → Statement Separation → Execution
```

Key features of the parsing approach:

- **State-based tokenization**: Tracks context (quotes, comments, etc.)
- **Recursive descent parsing**: Handles nested structures
- **Error recovery**: Continues parsing after errors when possible
- **Transaction management**: Maintains transaction integrity

### SQL Script Parsing

The `SqlScriptParser` handles:

1. **Comment removal**: Strips comments while preserving structure
2. **String literal handling**: Preserves quotes and their content
3. **Statement delimitation**: Identifies individual statements
4. **PL/SQL detection**: Special handling for procedural blocks

### Stored Procedure Parsing

The `StoredProcedureParser` handles:

1. **Parameter parsing**: Extracts parameter definitions
2. **Type determination**: Identifies parameter types (IN, OUT, INOUT)
3. **Validation**: Ensures syntactic correctness
4. **Function vs. Procedure**: Differentiates between types

## Error Handling

### Database Error Management
The application uses a sophisticated error handling system:

1. **Core Components**
   - DatabaseException: Central exception class
   - DatabaseErrorFormatter: Error translation and formatting
   - ConfigurationHolder: Configuration-based error mapping

2. **Error Classification**
   - Standard SQLState codes (ANSI SQL)
   - Vendor-specific error codes
   - Custom error types for application-level categorization

3. **Configuration**
   ```yaml
   databases:
     types:
       oracle:
         error-mappings:
           12154: ORACLE_TNS
           1017: CONN_AUTH
       postgresql:
         error-mappings:
           28000: CONN_AUTH
   ```

4. **Implementation Details**
   - Two-tier error handling approach
   - Configuration-driven vendor mappings
   - Rich error context preservation
   - Standardized error formatting

## Configuration System

The application uses a configuration system based on:

- **YAML files**: External configuration through YAML
- **Default configuration**: Sensible defaults when not specified
- **Runtime overrides**: Command-line options can override defaults
- **Configuration holder**: Singleton for application-wide access

## CLI Command Structure

The command-line interface supports:

```bash
# Basic SQL script execution
db -t oracle -H host -u username -p password -d database script.sql

# Stored procedure execution
db -t postgresql -H host -u username -p password -d database procedureName

# Validation without execution
db --pre-flight -t mysql -H host -u username -p password -d database script.sql
```

## Technical Constraints

### Performance Considerations

- **Memory Efficiency**: Processes SQL files line by line for large file support
- **Connection Pooling**: Reuses database connections when possible
- **Batch Processing**: Optional batching of SQL statements
- **Transaction Management**: Proper handling of commits and rollbacks

### Security Considerations

- **Password Handling**: Secure password management
- **Error Messages**: No sensitive information in errors
- **Input Validation**: Validates all inputs
- **Transaction Safety**: Prevents partial execution

## Development Workflow

### Inferred Development Process

1. **Feature Implementation**: Organized by component
2. **Exception Handling**: Comprehensive exception hierarchy
3. **Testing**: Likely uses JUnit with mocking
4. **Documentation**: Javadoc comments throughout codebase

## Deployment and Distribution

### Inferred Deployment Model

- **JAR Packaging**: Self-contained executable JAR
- **External Configuration**: YAML configuration files
- **JDBC Drivers**: Pluggable database drivers
- **CLI Interface**: Primary interaction method

## Database Layer
- JDBC-based database operations
- Supports multiple database types (Oracle, PostgreSQL, MySQL, SQL Server)
- Connection pooling and resource management
- Batch processing capabilities
- Result set streaming for large data sets

## Error Handling
- Custom DatabaseException with ErrorType enumeration
- Structured error messages and SQL state tracking
- Logging using Log4j2

## Testing Infrastructure
- Virtual thread support for concurrent testing
- CSV output for test results
- Configurable thread pools
