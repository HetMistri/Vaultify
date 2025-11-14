package com.vaultify.db;

import com.vaultify.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection management using centralized Config.
 * Supports both direct connection strings and component-based configuration.
 */
public class Database {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        loadConfig();
    }

    /**
     * Load database configuration from Config (which checks env vars first).
     * Supports both:
     * 1. Direct: db.url, db.user, db.password
     * 2. Component: db.host, db.port, db.name
     */
    private static void loadConfig() {
        // Try direct connection URL first
        DB_URL = Config.get("db.url");
        
        // If no direct URL, construct from components
        if (DB_URL == null || DB_URL.isEmpty() || DB_URL.startsWith("env.")) {
            String host = Config.get("db.host", "localhost");
            String port = Config.get("db.port", "5432");
            String name = Config.get("db.name", "vaultify_db");
            DB_URL = "jdbc:postgresql://" + host + ":" + port + "/" + name;
        }
        
        DB_USER = Config.get("db.user", "vaultify_user");
        DB_PASSWORD = Config.get("db.password", "secret123");

        System.out.println("Database URL: " + DB_URL);
        System.out.println("Database User: " + DB_USER);
    }

    /**
     * Get a database connection
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Test database connection
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection() {
        System.out.println("\n=== Testing Database Connection ===");
        System.out.println("Attempting to connect to: " + DB_URL);
        System.out.println("With user: " + DB_USER);

        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Connection established successfully!");
                System.out.println("✓ Database product: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("✓ Database version: " + conn.getMetaData().getDatabaseProductVersion());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("✗ Connection failed!");
            System.err.println("✗ Error: " + e.getMessage());
            return false;
        }

        return false;
    }

    /**
     * Main method for standalone testing
     */
    public static void main(String[] args) {
        boolean connected = testConnection();
        System.out.println("\n=================================");
        if (connected) {
            System.out.println("Status: READY - Database is accessible");
        } else {
            System.out.println("Status: NOT READY - Database is not accessible");
        }
        System.out.println("=================================\n");
        System.exit(connected ? 0 : 1);
    }
}
