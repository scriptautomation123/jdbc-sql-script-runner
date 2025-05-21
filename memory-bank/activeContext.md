# Active Context
<!-- markdownlint-disable MD022 MD032 MD022 MD02 MD009 MD047 MD028 MD037 MD040-->
## Current Focus

### Pattern-Based Development
- Implementing pattern-first approach for code generation
- Establishing clear guidelines for pattern application
- Documenting pattern evolution process
- Creating example implementations of common patterns
- Added optional transactional DML script execution via CLI flag
- Refactored script execution logic to partition DML and non-DML, and only wrap DML in a transaction if requested
- Centralized CLI test helpers in BaseDbTest for reuse
- Ensuring all module and artifact references are consistent across the multi-module Maven build

### Recent Changes
- Added comprehensive pattern application strategy
- Created pattern hierarchy for code generation
- Documented pattern evolution process
- Added concrete examples of pattern implementation
- Renamed executeScriptWithBatching to executeDmlScriptWithBatching for clarity
- Maintained separation between ResultSetStreamer and ResultSetProcessor
- Fixed UnifiedDatabaseOperation.Builder reference to UnifiedDatabaseOperationBuilder
- Enhanced error handling in DatabaseLoginService
- Added --transactional CLI flag for DML script execution (default: non-transactional)
- Refactored UnifiedDatabaseOperation and UnifiedDatabaseRunner for transactional DML support
- Added/updated integration tests for transactional and non-transactional DML execution
- Moved CLI test helpers to BaseDbTest for reuse
- Fixed module dependency naming: replaced all references to 'shdemmo-app' with 'dbscriptrunner' in all POM files
- Resolved build failure in database-login-validation-tool by correcting app module dependency

### Next Steps
1. Apply pattern-first approach to upcoming features
2. Review existing code for pattern compliance
3. Document any new patterns discovered
4. Create additional pattern examples as needed
5. Expand test coverage for new CLI features and transactional logic
6. Update documentation to reflect new CLI options and behaviors

### Active Decisions
1. Using pattern-first development approach
2. Following established pattern hierarchy
3. Maintaining pattern documentation
4. Evolving patterns based on new requirements

## Recent Changes

Recent work on the codebase has involved:

1. **SQL Script Parser Improvements**
   - Added support for nested PL/SQL BEGIN/END blocks
   - Enhanced comment handling to preserve structure
   - Improved string literal detection to avoid false statement boundaries

2. **Command Line Interface Enhancements**
   - Added pre-flight validation mode
   - Implemented explain plan generation
   - Added support for stored procedure parameter passing

3. **Error Handling Updates**
   - Consolidated multiple database exception classes into a single `DatabaseException`
   - Implemented a generic-first approach using SQLState codes with vendor-specific fallbacks
   - Moved error mappings to configuration (application.yaml)
   - Enhanced error context with both standard and vendor-specific information

## Next Steps

The immediate next steps for the project are:

1. **Testing Expansion**
   - Create comprehensive unit tests for parser edge cases
   - Implement integration tests with actual database systems
   - Develop performance benchmarks for parser operations

2. **Documentation**
   - Complete internal code documentation
   - Create user guide for CLI operations
   - Document SQL dialect-specific features and limitations

3. **Feature Additions**
   - Implement CSV export functionality for query results
   - Add support for additional database types
   - Create basic configuration options for parser behavior

## Active Decisions

Several key decisions are currently being considered:

1. **Parsing Strategy Trade-offs**
   - Evaluating line-by-line vs. complete file parsing for different scenarios
   - Considering impacts of different tokenization approaches on memory usage
   - Assessing error recovery vs. performance trade-offs

2. **API Design Considerations**
   - Determining the right level of abstraction for parser interfaces
   - Evaluating how to expose parser functionality to external callers
   - Considering backward compatibility for future changes

3. **Extension Mechanisms**
   - Designing plugin architecture for dialect support
   - Evaluating extension points for custom validators
   - Considering configuration-driven vs. code-driven extensibility

## Development Constraints

The development is proceeding with these constraints in mind:

1. **Performance Requirements**
   - Parser must handle files up to 100MB with reasonable memory usage
   - CLI operations should complete within acceptable timeframes
   - Large batch operations must remain memory-efficient

2. **Compatibility Requirements**
   - Must support Java 8+ runtime environments
   - Should handle multiple JDBC driver versions
   - Need to maintain consistent behavior across operating systems

3. **Security Considerations**
   - Must handle credentials securely
   - Should prevent SQL injection in dynamic operations
   - Need to ensure proper transaction isolation
