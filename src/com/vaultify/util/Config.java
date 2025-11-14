package com.vaultify.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration management.
 * Priority: Environment variables → config.properties → defaults
 * Uses classpath loading to work in JAR/Docker environments.
 */
public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    /**
     * Get a configuration value with environment variable override.
     * First checks environment (using ENV_VAR_NAME format), then config.properties.
     * 
     * @param key The property key (e.g., "db.url")
     * @return The configuration value, or null if not found
     */
    public static String get(String key) {
        // Convert property key to environment variable format (db.url → DB_URL)
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        
        return props.getProperty(key);
    }

    /**
     * Get a configuration value with a default fallback.
     * 
     * @param key The property key
     * @param defaultValue Value to return if key not found
     * @return The configuration value or default
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Get an integer configuration value.
     * 
     * @param key The property key
     * @param defaultValue Value to return if key not found or invalid
     * @return The integer value
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
