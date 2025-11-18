package com.vaultify.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Final configuration loader:
 * 1. Loads config.properties from classpath
 * 2. Overrides with environment variables (DB_HOST, DB_PORT, ...)
 */
public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties missing in resources/");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    /** Convert db.host → DB_HOST */
    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);

        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        return props.getProperty(key);
    }


    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
