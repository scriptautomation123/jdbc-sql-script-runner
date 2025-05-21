package com.example.shelldemo.util;

import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

/**
 * Utility class for secure logging in database operations.
 * Handles sensitive information to ensure it's not accidentally logged.
 */
public final class LoggingUtils {
    
    private static final String[] SENSITIVE_PROPERTY_KEYS = {
        "password", "passwd", "pwd", "secret", "token", "credentials", "key"
    };
    
    private LoggingUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Logs database connection information without exposing sensitive data.
     * 
     * @param logger The logger to use
     * @param url The JDBC URL (may contain sensitive information)
     * @param props The connection properties (may contain sensitive information)
     */
    public static void logSensitiveConnectionInfo(Logger logger, String url, Properties props) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        // Log connection URL with any potential password parts masked
        String maskedUrl = maskSensitiveUrlParts(url);
        logger.debug("Connection URL: {}", maskedUrl);
        
        // Log connection properties without sensitive values
        if (props != null && !props.isEmpty()) {
            String maskedProps = props.stringPropertyNames().stream()
                .filter(key -> !isSensitiveKey(key))
                .map(key -> key + "=" + props.getProperty(key))
                .collect(Collectors.joining(", "));
            
            int sensitiveCount = (int) props.stringPropertyNames().stream()
                .filter(LoggingUtils::isSensitiveKey)
                .count();
                
            logger.debug("Connection properties: {} (plus {} sensitive properties)", 
                maskedProps, sensitiveCount);
        }
    }
    
    /**
     * Checks if a property key contains sensitive information that should not be logged.
     * 
     * @param key The property key to check
     * @return true if the key contains sensitive information
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_PROPERTY_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Masks sensitive parts in database connection URLs.
     * 
     * @param url The JDBC URL to mask
     * @return The masked URL
     */
    public static String maskSensitiveUrlParts(String url) {
        if (url == null) {
            return "null";
        }
        
        // Common patterns for passwords in URLs
        return url.replaceAll("(?i)password=([^&;]*)", "password=*****")
                 .replaceAll("(?i)pwd=([^&;]*)", "pwd=*****")
                 .replaceAll("(?i):([^:/@]+)@", ":*****@");
    }
    
    /**
     * Standardized logging for database operations with optional parameters.
     * 
     * @param logger The logger to use
     * @param operation The operation description (e.g., "query", "update")
     * @param params Optional parameters to include in the log message
     */
    public static void logOperation(Logger logger, String operation, Object... params) {
        if (logger.isInfoEnabled()) {
            logger.info("Executing {}: {}", operation, 
                    params.length > 0 ? params[0] : "");
        }
    }
    
    /**
     * Logs SQL statements safely with truncation for very long statements.
     * 
     * @param logger The logger to use
     * @param sql The SQL statement to log
     */
    public static void logSql(Logger logger, String sql) {
        if (logger.isDebugEnabled()) {
            // Truncate SQL if too long
            String trimmedSql = sql.length() > 500 
                    ? sql.substring(0, 500) + "..." 
                    : sql;
            logger.debug("Executing SQL: {}", trimmedSql);
        }
    }
    
    /**
     * Logs the completion of an operation with timing information.
     * 
     * @param logger The logger to use
     * @param operation The operation description
     * @param startTimeMs The operation start time in milliseconds
     */
    public static void logOperationComplete(Logger logger, String operation, long startTimeMs) {
        if (logger.isInfoEnabled()) {
            long duration = System.currentTimeMillis() - startTimeMs;
            logger.info("{} completed in {}ms", operation, duration);
        }
    }
    
    /**
     * Log a debug message
     *
     * @param logger The logger to use
     * @param message The message to log
     */
    public static void debug(Logger logger, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }
    
    /**
     * Log a debug message with one parameter
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param The parameter
     */
    public static void debug(Logger logger, String message, Object param) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, param);
        }
    }
    
    /**
     * Log a debug message with two parameters
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param1 The first parameter
     * @param param2 The second parameter
     */
    public static void debug(Logger logger, String message, Object param1, Object param2) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, param1, param2);
        }
    }
    
    /**
     * Log an info message
     *
     * @param logger The logger to use
     * @param message The message to log
     */
    public static void info(Logger logger, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }
    
    /**
     * Log an info message with one parameter
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param The parameter
     */
    public static void info(Logger logger, String message, Object param) {
        if (logger.isInfoEnabled()) {
            logger.info(message, param);
        }
    }
    
    /**
     * Log an info message with two parameters
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param1 The first parameter
     * @param param2 The second parameter
     */
    public static void info(Logger logger, String message, Object param1, Object param2) {
        if (logger.isInfoEnabled()) {
            logger.info(message, param1, param2);
        }
    }
    
    /**
     * Log a warning message
     *
     * @param logger The logger to use
     * @param message The message to log
     */
    public static void warn(Logger logger, String message) {
        logger.warn(message);
    }
    
    /**
     * Log a warning message with one parameter
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param The parameter
     */
    public static void warn(Logger logger, String message, Object param) {
        logger.warn(message, param);
    }
    
    /**
     * Log a warning message with two parameters
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param1 The first parameter
     * @param param2 The second parameter
     */
    public static void warn(Logger logger, String message, Object param1, Object param2) {
        logger.warn(message, param1, param2);
    }
    
    /**
     * Log an error message
     *
     * @param logger The logger to use
     * @param message The message to log
     */
    public static void error(Logger logger, String message) {
        logger.error(message);
    }
    
    /**
     * Log an error message with one parameter
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param The parameter
     */
    public static void error(Logger logger, String message, Object param) {
        logger.error(message, param);
    }
    
    /**
     * Log an error message with two parameters
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param1 The first parameter
     * @param param2 The second parameter
     */
    public static void error(Logger logger, String message, Object param1, Object param2) {
        logger.error(message, param1, param2);
    }
    
    /**
     * Log an error message with a throwable
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param throwable The throwable
     */
    public static void error(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    /**
     * Log an error message with one parameter and a throwable
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param The parameter
     * @param throwable The throwable
     */
    public static void error(Logger logger, String message, Object param, Throwable throwable) {
        logger.error(message, param, throwable);
    }
    
    /**
     * Log an error message with two parameters and a throwable
     *
     * @param logger The logger to use
     * @param message The message to log
     * @param param1 The first parameter
     * @param param2 The second parameter
     * @param throwable The throwable
     */
    public static void error(Logger logger, String message, Object param1, Object param2, Throwable throwable) {
        logger.error(String.format(message, param1, param2), throwable);
    }
}
