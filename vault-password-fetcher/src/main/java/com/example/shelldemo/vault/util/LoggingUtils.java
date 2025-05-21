package com.example.shelldemo.vault.util;

import org.apache.logging.log4j.Logger;


public final class LoggingUtils {
    private static final String[] SENSITIVE_KEYS = {
        "password", "passwd", "pwd", "secret", "token", "credentials", "key", "secret_id"
    };
    
    private LoggingUtils() {
        // Private constructor to prevent instantiation
    }
    
    public static String maskSensitiveValue(String value) {
        if (value == null) {
            return "null";
        }
        return "*****";
    }
    
    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }
    
    public static void logSensitiveInfo(Logger logger, String message, Object... args) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        // Mask any sensitive values in the args
        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String && isSensitiveKey(args[i].toString())) {
                maskedArgs[i] = maskSensitiveValue(args[i].toString());
            } else {
                maskedArgs[i] = args[i];
            }
        }
        
        logger.debug(message, maskedArgs);
    }
} 