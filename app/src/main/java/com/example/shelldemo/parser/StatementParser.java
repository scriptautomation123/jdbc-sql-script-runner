package com.example.shelldemo.parser;

import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.example.shelldemo.spi.DatabaseVendor;

/**
 * Parser for SQL statements that handles different SQL statement types
 * including regular SQL and PL/SQL blocks.
 */
public class StatementParser {
    private static final Logger logger = LogManager.getLogger(StatementParser.class);
    
    private final Map<Integer, String> statements;
    private StringBuilder currentStatement = new StringBuilder();
    private boolean inPlsqlBlock = false;
    private int plsqlLevel = 0;
    private int statementCount = 0;
    private final DatabaseVendor vendor;
    
    /**
     * Creates a new StatementParser instance.
     * 
     * @param statements the map to store parsed statements
     * @param vendor the database vendor
     */
    public StatementParser(Map<Integer, String> statements, DatabaseVendor vendor) {
        this.statements = statements;
        this.vendor = vendor;
    }
    
    /**
     * Processes a line of SQL code.
     * 
     * @param line the line to process
     */
    public void processLine(String line) {
        if (handlePlSqlTerminator(line)) return;
        if (handlePlSqlBlockStart(line)) return;
        if (inPlsqlBlock) {
            processPlSqlLine(line);
        } else {
            processRegularLine(line);
        }
    }
    
    private boolean handlePlSqlTerminator(String line) {
        if (!line.equals("/")) return false;
        
        if (inPlsqlBlock) {
            addCurrentStatement();
            inPlsqlBlock = false;
            plsqlLevel = 0;
        } else if (currentStatement.length() > 0) {
            currentStatement.append(line).append("\n");
        }
        return true;
    }
    
    private boolean handlePlSqlBlockStart(String line) {
        if (inPlsqlBlock || !isPLSQLBlockStart(line)) return false;
        
        inPlsqlBlock = true;
        plsqlLevel = 1;
        
        if (currentStatement.length() > 0) {
            addCurrentStatement();
        }
        
        currentStatement.append(line).append("\n");
        return true;
    }
    
    private void processPlSqlLine(String line) {
        plsqlLevel = updatePLSQLBlockLevel(line, plsqlLevel);
        currentStatement.append(line).append("\n");
        
        if (plsqlLevel <= 0) {
            addCurrentStatement();
            inPlsqlBlock = false;
        }
    }
    
    private void processRegularLine(String line) {
        currentStatement.append(line);
        
        if (line.endsWith(";")) {
            addCurrentStatement();
        } else {
            currentStatement.append("\n");
        }
    }
    
    private void addStatement(int statementCount, StringBuilder currentStatement) {
        String stmt = currentStatement.toString().trim();
        if (!stmt.isEmpty()) {
            if (!vendor.isPLSQL(stmt) && stmt.endsWith(";")) {
                stmt = stmt.substring(0, stmt.length() - 1).trim();
            }
            statements.put(statementCount, stmt);
        }
        currentStatement.setLength(0);
    }
    
    private void addCurrentStatement() {
        addStatement(++statementCount, currentStatement);
    }
    
    /**
     * Adds any remaining statement that hasn't been added yet.
     */
    public void addRemainingStatement() {
        if (currentStatement.length() > 0) {
            addCurrentStatement();
        }
        logger.info("Successfully parsed {} SQL statements", statements.size());
    }

    private boolean isPLSQLBlockStart(String line) {
        String upperLine = line.toUpperCase();
        return upperLine.startsWith("BEGIN") || 
               upperLine.startsWith("DECLARE") ||
               (upperLine.startsWith("CREATE") && 
                (upperLine.contains(" FUNCTION") || 
                 upperLine.contains(" PROCEDURE") || 
                 upperLine.contains(" TRIGGER") || 
                 upperLine.contains(" PACKAGE")));
    }

    private int updatePLSQLBlockLevel(String line, int level) {
        String upperLine = line.toUpperCase();
        if (upperLine.contains("BEGIN")) {
            level++;
        } else if (upperLine.contains("END;")) {
            level--;
        }
        return level;
    }
} 