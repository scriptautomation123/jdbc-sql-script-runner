# JDBC Runner Refactoring

This refactoring implements a comprehensive modernization of the JDBC Runner codebase to follow Java 21 best practices, enhance modularity, and improve maintainability.

## Key Components

### Core Components

1. **DatabaseContext** - Central context for database operations
2. **TransactionManager** - Improved transaction handling
3. **UnifiedDatabaseOperation** - Simplified facade delegating to specialized executors

### Execution Components

1. **SqlExecutor** - Sealed interface for SQL execution
2. **QueryExecutor** - Handles SELECT statements
3. **DmlExecutor** - Handles INSERT/UPDATE/DELETE statements
4. **DdlExecutor** - Handles CREATE/ALTER/DROP statements
5. **ProcedureExecutor** - Handles stored procedures

### SPI Layer

1. **DatabaseVendor** - Enhanced interface for vendor-specific logic
2. **VendorRegistry** - Improved registry with better error handling

### SQL Processing

1. **SqlStatement** - Expanded sealed interface hierarchy
2. **SqlStatementFactory** - Factory for SQL statements
3. **VirtualThreadBatchExecutor** - Uses Java 21 virtual threads for batch operations

### Result Processing

1. **ResultProcessor** - Generic interface for result set processing
2. **MapResultProcessor** - Processes result sets into maps
3. **ResultProcessingPipeline** - Combines processing and streaming

## Java 21 Features

This refactoring leverages several Java 21 features:

1. **Record Classes** - Used for immutable data objects
2. **Sealed Classes** - Used for type-safe hierarchies
3. **Pattern Matching** - Used for type-safe casting
4. **Virtual Threads** - Used for efficient concurrent processing
5. **Switch Expressions** - Used for cleaner conditional logic

## How to Transition (Gradual Approach)

The refactoring is designed to allow for a gradual transition:

1. **Phase 1** - Use both old and new code
   - The new classes have a "2" suffix
   - Existing components interact with both systems

2. **Phase 2** - Switch to new implementation
   - Update import statements
   - Remove the "2" suffix from class names

3. **Phase 3** - Remove legacy classes
   - Remove deprecated classes
   - Update remaining references

## Benefits

1. **Improved Readability** - Smaller, focused classes
2. **Enhanced Maintainability** - Better separation of concerns
3. **Better Testability** - Proper dependency injection
4. **Reduced Duplication** - Common code extracted
5. **Modern Java Usage** - Leveraging Java 21 features
6. **Consistency** - Standardized approach to logging and error handling

## Usage Examples

### Using the New API

```java
// Create a database context
DatabaseContext context = new DatabaseContext.Builder()
    .connection(connection)
    .vendor(vendor)
    .configService(new ConfigHolderAdapter())
    .dbType("oracle")
    .build();

// Create a UnifiedDatabaseOperation
UnifiedDatabaseOperation db = new UnifiedDatabaseOperation(context);

// Execute a query
List<Map<String, Object>> results = db.executeQuery("SELECT * FROM employees");

// Execute an update
int rowsAffected = db.executeUpdate("UPDATE employees SET salary = 5000 WHERE id = 1");

// Execute a stored procedure
Map<String, Object> outParams = db.callStoredProcedure("get_employee_info",
    List.of(new ProcedureParam("p_id", "NUMBER", 1)),
    List.of(new ProcedureParam("p_name", "VARCHAR")));

// Use try-with-resources
try (UnifiedDatabaseOperation db = new UnifiedDatabaseOperationBuilder()
        .host("localhost")
        .port(1521)
        .username("user")
        .password("pass")
        .dbType("oracle")
        .serviceName("XE")
        .build()) {

    // Perform operations
}
```

### Using Specialized Executors Directly

```java
// Create executors
QueryExecutor queryExecutor = new QueryExecutor(context);
DmlExecutor dmlExecutor = new DmlExecutor(context);

// Create statements
SqlStatement.QueryStatement query = new SqlStatement.QueryStatement("SELECT * FROM employees");
SqlStatement.DmlStatement update = new SqlStatement.DmlStatement("UPDATE employees SET salary = 5000");

// Execute statements
List<Map<String, Object>> results = (List<Map<String, Object>>) queryExecutor.execute(query);
int rowsAffected = (Integer) dmlExecutor.execute(update);
```

### Using Virtual Thread Batch Processing

```java
VirtualThreadBatchExecutor batchExecutor = new VirtualThreadBatchExecutor(
    context, statementFactory, 60, true);

List<String> statements = List.of(
    "INSERT INTO employees VALUES (1, 'John')",
    "INSERT INTO employees VALUES (2, 'Jane')"
);

int executed = batchExecutor.executeBatch(statements, ProcessingMode.CONCURRENT);
```

## Current Progress (May 14, 2025)

### Completed

1. **Core Framework**
   - Defined enhanced interfaces and implementations
   - Created modular executor components
   - Implemented centralized exception handling

2. **Database Vendor Support**
   - Enhanced `DatabaseVendor2` interface with additional functionality
   - Implemented vendor-specific classes for Oracle, PostgreSQL, MySQL, and SQL Server
   - Created `VendorRegistry` for dynamic loading of vendor implementations
   - Added comprehensive test coverage

3. **Unit Testing**
   - Created test cases for vendor registry
   - Added extensive tests for Oracle implementation

### In Progress

1. **Migration Strategy**
   - Rename final classes (remove the "2" suffix)
   - Phase out legacy classes
   - Update imports throughout the codebase

2. **Integration Testing**
   - End-to-end tests with different database vendors
   - Performance testing for batch operations

### Next Steps

1. **Complete the Connection Management**
   - Implement connection pooling improvements
   - Add connection validation and monitoring

2. **Comprehensive Documentation**
   - Create detailed transition guide for existing users
   - Add code examples and usage patterns

3. **Performance Optimizations**
   - Fine-tune the virtual thread implementation
   - Optimize prepared statement caching

## Vendor-Agnostic Integration Testing

The refactored test suite is designed to be **vendor-agnostic**:

- No hard-coded vendor configuration in shared setup methods.
- All vendor-specific details (host, port, username, etc.) are provided by parameterized test providers (e.g., `vendorAndConnectionProvider`).
- Each test run sets up the configuration for the specific vendor being tested, inside the test method itself.
- The same test logic is executed for all supported vendors (Oracle, PostgreSQL, etc.).

### How It Works
- The main integration test class, `UnifiedDatabaseRunnerTest`, uses a parameterized provider to supply connection details for each vendor.
- The test method sets up the configuration for the current vendor using the parameters from the provider.
- This approach ensures that tests are easy to extend to new vendors and remain maintainable.

#### Example: Adding a New Vendor

To add a new vendor (e.g., MySQL) to the integration tests:

```java
static Stream<Arguments> vendorAndConnectionProvider() {
    return Stream.of(
        Arguments.of(
            "oracle", ORACLE_HOST, ORACLE_PORT, ORACLE_DATABASE, ORACLE_USERNAME, ORACLE_PASSWORD, "SELECT 1 FROM DUAL", "1"
        ),
        Arguments.of(
            "postgresql", "localhost", 5432, "testdb", "postgres", "postgres", "SELECT 1", "1"
        ),
        Arguments.of(
            "mysql", "localhost", 3306, "testdb", "root", "root", "SELECT 1", "1"
        )
    ).filter(args -> isVendorEnabled((String) args.get()[0]));
}
```

The test method will automatically run for the new vendor, using the provided parameters.

### Benefits
- **Maintainability:** No duplicated setup logic for each vendor.
- **Extensibility:** Add new vendors by updating the provider.
- **Consistency:** All vendors are tested with the same logic.

See `UnifiedDatabaseRunnerTest.java` for a real-world example.
