# Project Brief: Database Management Tool
<!-- markdownlint-disable MD022 MD032 MD022 MD02 MD009 MD047 MD028-->
## Overview
This project is a comprehensive database management and SQL execution tool. It provides a unified command-line interface for interacting with multiple database systems, executing SQL scripts, calling stored procedures, and validating SQL syntax. The tool includes robust SQL parsing capabilities that handle multiple SQL dialects, comments, string literals, and statement delimiters.

## Core Requirements

### Database Connectivity
- Connect to multiple database types (Oracle, SQL Server, PostgreSQL, MySQL)
- Support different connection methods (e.g., thin, LDAP for Oracle)
- Manage database connections securely

### SQL Execution
- Execute SQL scripts with proper transaction handling
- Run individual SQL statements with parameter binding
- Call stored procedures and functions with parameter support
- Maintain transaction integrity (commit/rollback)

### SQL Parsing
- Parse SQL script files into individual statements
- Handle SQL comments, quoted strings, and statement delimiters
- Support dialect-specific syntax (e.g., PL/SQL blocks)
- Provide robust error handling with meaningful messages

### Validation
- Pre-flight validation of SQL scripts without execution
- Generate execution plans for performance analysis
- Verify syntax correctness across different SQL dialects

### Command-Line Interface
- Provide a user-friendly CLI with clear options
- Support multiple operation modes (script execution, stored procedure calls)
- Include comprehensive help and documentation

## Goals
- Create a reliable, cross-database management tool for developers and DBAs
- Ensure consistent behavior across different database systems
- Provide detailed error messages and logging for troubleshooting
- Support batch operations for efficiency
- Enable safe SQL script execution with transaction integrity

## Architecture Principles
- Clean separation of concerns (parsing, validation, execution)
- Extensible design for adding new database types
- Comprehensive error handling and logging
- Configuration-driven behavior with sensible defaults
- Factory pattern for creating appropriate database connections
- Command pattern for executing operations

## Note
This brief is based on initial code exploration and may need refinement as more context becomes available. 