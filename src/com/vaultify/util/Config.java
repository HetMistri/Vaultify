package com.vaultify.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader with layered approach:
 * 1. Loads .env file (for sensitive credentials like DB passwords, API keys)
 * 2. Loads config.properties from classpath (for non-sensitive defaults)
 * 3. Allows environment variable overrides
 * 
 * Priority: System.getenv() > .env > config.properties
 */
public class Config {
    private static final Properties props = new Properties();
    private static final Dotenv dotenv;

    static {
        // Load .env file (ignores if missing, to allow running without .env in Docker)
        dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Load config.properties for non-sensitive defaults
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties missing in resources/");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        // Convert key format: db.url -> DB_URL
        String envKey = key.toUpperCase().replace('.', '_');
        
        // Priority 1: System environment variables (highest priority)
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Priority 2: .env file
        String dotenvValue = dotenv.get(envKey);
        if (dotenvValue != null && !dotenvValue.isEmpty()) {
            return dotenvValue;
        }

        // Priority 3: config.properties (fallback)
        return props.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String value = get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static boolean isDevMode() {
        return getBoolean("dev.mode", false);
    }
}
