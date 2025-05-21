package com.example.shelldemo.validate;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.parser.SqlScriptParser.ParamType;

public class StoredProcedureValidator {
    public static void validateParts(String[] parts, ParamType type, int paramIndex) {
        if (parts.length < 2) {
            throw new DatabaseException(
                String.format(
                    "Invalid parameter format at position %d. Expected 'name:type[:value]', got '%s'",
                    paramIndex,
                    String.join(":", parts)
                ),
                ErrorType.PARSE_PROCEDURE
            );
        }

        if ((type == ParamType.IN || type == ParamType.INOUT) && parts.length < 3) {
            throw new DatabaseException(
                String.format(
                    "Missing value for %s parameter at position %d",
                    type,
                    paramIndex
                ),
                ErrorType.PARSE_PROCEDURE
            );
        }

        if (type == ParamType.OUT && parts.length > 2) {
            throw new DatabaseException(
                String.format(
                    "OUT parameter at position %d should not have a value",
                    paramIndex
                ),
                ErrorType.PARSE_PROCEDURE
            );
        }
    }

    public static void validateDefinition(String definition) {
        if (definition == null || definition.trim().isEmpty()) {
            throw new DatabaseException(
                "Stored procedure definition cannot be null or empty",
                ErrorType.PARSE_PROCEDURE
            );
        }
    }
} 