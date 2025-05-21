package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.exception.ProcedureParseException;
import com.example.shelldemo.spi.DatabaseVendor;


/**
 * Unified SQL parsing utility class.
 * Handles SQL scripts, PL/SQL blocks, stored procedures, and functions.
 */
public final class SqlScriptParser {
    private static final Logger logger = LogManager.getLogger(SqlScriptParser.class);
    private static final String PARAM_DELIMITER = ",";
    private static final String FIELD_DELIMITER = ":";
    private static final Pattern PROCEDURE_PATTERN = Pattern.compile(
        "CREATE\\s+(OR\\s+REPLACE\\s+)?(FUNCTION|PROCEDURE)\\s+([^(]+)\\(([^)]*)\\)",
        Pattern.CASE_INSENSITIVE
    );

    private SqlScriptParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Parameter types for stored procedures.
     */
    public enum ParamType {
        IN("IN"),
        OUT("OUT"),
        INOUT("IN/OUT");

        private final String description;

        ParamType(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Represents a stored procedure parameter.
     */
    public static final class ProcedureParam {
        private final String name;
        private final String type;
        private final String value;
        private final ParamType paramType;

        private ProcedureParam(Builder builder) {
            this.name = Objects.requireNonNull(builder.name, "Parameter name cannot be null");
            this.type = Objects.requireNonNull(builder.type, "Parameter type cannot be null");
            this.value = builder.value; // value can be null for OUT parameters
            this.paramType = Objects.requireNonNull(builder.paramType, "Parameter direction cannot be null");
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getValue() { return value; }
        public ParamType getParamType() { return paramType; }

        @Override
        public String toString() {
            return String.format("%s %s %s%s", 
                paramType, name, type, 
                value != null ? " = " + value : "");
        }

        public static class Builder {
            private String name;
            private String type;
            private String value;
            private ParamType paramType;

            public Builder name(String name) { this.name = name; return this; }
            public Builder type(String type) { this.type = type; return this; }
            public Builder value(String value) { this.value = value; return this; }
            public Builder paramType(ParamType paramType) { this.paramType = paramType; return this; }

            public ProcedureParam build() {
                return new ProcedureParam(this);
            }
        }
    }

    /**
     * Represents stored procedure information.
     */
    public static record StoredProcedureInfo(String name, String parameters) {}

    private static final class CommentParserState {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        private int multiLineCommentDepth = 0;
        
        boolean inAnyString() {
            return inSingleQuote || inDoubleQuote;
        }
    }

    /**
     * Record representing the result of processing a character in SQL parsing.
     * 
     * @param appendChar Whether to append the current character to the result
     * @param indexShift How many positions to shift the index after processing
     */
    private static record ProcessResult(boolean appendChar, int indexShift) {
        static ProcessResult skip(int indexShift) {
            return new ProcessResult(false, indexShift);
        }
        
        static ProcessResult append() {
            return new ProcessResult(true, 0);
        }
    }

    /**
     * Validates that a string is not null or empty after trimming.
     */
    private static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            String errorMessage = fieldName + " cannot be null or empty";
            throw new DatabaseException(errorMessage, ErrorType.PARSE_SQL);
        }
    }

    /**
     * Parses a SQL script file into a map of individual SQL statements.
     */
    public static Map<Integer, String> parseSqlFile(File scriptFile, DatabaseVendor vendor) throws DatabaseException {
        validateScriptFile(scriptFile);
        
        logger.debug("Starting SQL file parsing: {}", scriptFile.getName());

        try {
            String content = new String(Files.readAllBytes(scriptFile.toPath()));
            String processedContent = removeComments(content);
            return parseStatements(processedContent, vendor);
        } catch (IOException e) {
            throw new DatabaseException(
                "Failed to parse SQL file: " + scriptFile.getName(), 
                e, ErrorType.PARSE_SQL
            );
        } catch (IllegalArgumentException e) {
            throw new DatabaseException(
                e.getMessage(), 
                e, ErrorType.PARSE_SQL
            );
        }
    }

    /**
     * Parses a stored procedure or function definition.
     */
    public static StoredProcedureInfo parseStoredProcedure(String definition) {
        logger.debug("Parsing stored procedure definition");
        validateNotEmpty(definition, "Stored procedure definition");
        
        Matcher matcher = PROCEDURE_PATTERN.matcher(definition);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid stored procedure definition format", ErrorType.PARSE_PROCEDURE);
        }

        String procedureName = matcher.group(3).trim();
        String parameters = matcher.group(4).trim();
        
        logger.debug("Successfully parsed stored procedure: {}", procedureName);
        return new StoredProcedureInfo(procedureName, parameters);
    }

    /**
     * Parses procedure parameters.
     */
    public static List<ProcedureParam> parseProcedureParams(
            String inputParams, String outputParams, String ioParams) {
        List<ProcedureParam> params = new ArrayList<>();
        try {
            if (inputParams != null && !inputParams.trim().isEmpty()) {
                params.addAll(parseParamString(inputParams, ParamType.IN));
            }
            if (outputParams != null && !outputParams.trim().isEmpty()) {
                params.addAll(parseParamString(outputParams, ParamType.OUT));
            }
            if (ioParams != null && !ioParams.trim().isEmpty()) {
                params.addAll(parseParamString(ioParams, ParamType.INOUT));
            }
            return params;
        } catch (IllegalArgumentException e) {
            throw new ProcedureParseException("Invalid parameter format", e);
        } catch (RuntimeException e) {
            throw new ProcedureParseException("Failed to parse procedure parameters", e);
        }
    }

    private static List<ProcedureParam> parseParamString(String paramString, ParamType type) {
        List<ProcedureParam> params = new ArrayList<>();
        String[] paramPairs = paramString.trim().split(PARAM_DELIMITER);
        
        for (int i = 0; i < paramPairs.length; i++) {
            String pair = paramPairs[i].trim();
            if (pair.isEmpty()) continue;

            String[] parts = pair.split(FIELD_DELIMITER);
            validateParamParts(parts, type, i + 1);

            params.add(new ProcedureParam.Builder()
                .name(parts[0].trim())
                .type(parts[1].trim())
                .value(parts.length > 2 ? parts[2].trim() : null)
                .paramType(type)
                .build());
        }
        
        return params;
    }

    private static void validateScriptFile(File scriptFile) {
        if (scriptFile == null) {
            throw new DatabaseException("Script file cannot be null", ErrorType.PARSE_SQL);
        }
        if (!scriptFile.exists()) {
            throw new DatabaseException("Script file does not exist: " + scriptFile.getPath(), ErrorType.PARSE_SQL);
        }
        if (!scriptFile.isFile()) {
            throw new DatabaseException("Path is not a file: " + scriptFile.getPath(), ErrorType.PARSE_SQL);
        }
    }

    private static void validateParamParts(String[] parts, ParamType type, int paramIndex) {
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

    private static ProcessResult processChar(char c, char next, CommentParserState state) {
        if (isLineCommentEnd(c, state)) {
            return handleLineCommentEnd(state);
        }

        if (isNestedCommentStart(c, next, state)) {
            return handleNestedCommentStart(state);
        }

        if (isNestedCommentEnd(c, next, state)) {
            return handleNestedCommentEnd(state);
        }

        if (isInComment(state)) {
            return ProcessResult.skip(0);
        }

        if (isStringDelimiter(c, state)) {
            processStringDelimiter(c, state);
            return ProcessResult.append();
        }

        if (isLineCommentStart(c, next, state)) {
            return handleLineCommentStart(state);
        }

        return ProcessResult.append();
    }

    private static boolean isLineCommentEnd(char c, CommentParserState state) {
        return isLineEnd(c) && state.inLineComment;
    }

    private static ProcessResult handleLineCommentEnd(CommentParserState state) {
        state.inLineComment = false;
        return ProcessResult.append();
    }

    private static boolean isNestedCommentStart(char c, char next, CommentParserState state) {
        return !state.inAnyString() && c == '/' && next == '*';
    }

    private static ProcessResult handleNestedCommentStart(CommentParserState state) {
        state.multiLineCommentDepth++;
        return ProcessResult.skip(1);
    }

    private static boolean isNestedCommentEnd(char c, char next, CommentParserState state) {
        return !state.inAnyString() && c == '*' && next == '/' && state.multiLineCommentDepth > 0;
    }

    private static ProcessResult handleNestedCommentEnd(CommentParserState state) {
        state.multiLineCommentDepth--;
        return ProcessResult.skip(1);
    }

    private static boolean isInComment(CommentParserState state) {
        return state.inLineComment || state.multiLineCommentDepth > 0;
    }

    private static boolean isLineCommentStart(char c, char next, CommentParserState state) {
        return !state.inAnyString() && c == '-' && next == '-';
    }

    private static ProcessResult handleLineCommentStart(CommentParserState state) {
        state.inLineComment = true;
        return ProcessResult.skip(1);
    }

    private static String removeComments(String content) {
        StringBuilder processed = new StringBuilder();
        CommentParserState state = new CommentParserState();
        int i = 0;
        
        while (i < content.length()) {
            char c = content.charAt(i);
            char next = (i < content.length() - 1) ? content.charAt(i + 1) : '\0';
            
            ProcessResult result = processChar(c, next, state);
            if (result.appendChar) {
                processed.append(c);
            }
            
            i += 1 + result.indexShift;
        }
        
        return processed.toString();
    }
    
    private static boolean isLineEnd(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean isStringDelimiter(char c, CommentParserState state) {
        return (c == '\'' && !state.inDoubleQuote) || (c == '"' && !state.inSingleQuote);
    }

    private static void processStringDelimiter(char c, CommentParserState state) {
        if (c == '\'') {
            state.inSingleQuote = !state.inSingleQuote;
        } else if (c == '"') {
            state.inDoubleQuote = !state.inDoubleQuote;
        }
    }

    private static Map<Integer, String> parseStatements(String content, DatabaseVendor vendor) {
        Map<Integer, String> statements = new HashMap<>();
        String[] lines = content.replace("\r\n", "\n").split("\n");
        StatementParser parser = new StatementParser(statements, vendor);
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            parser.processLine(line);
        }
        
        parser.addRemainingStatement();
        return statements;
    }
}