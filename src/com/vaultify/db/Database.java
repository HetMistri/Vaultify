package com.vaultify.db;

import com.vaultify.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // Get settings from your Config.java (which checks env vars first)
    private static final String URL = Config.get("db.url", "jdbc:postgresql://localhost:5432/vaultify");
    private static final String USER = Config.get("db.user", "postgres");
    private static final String PASSWORD = Config.get("db.password", "admin");

    /**
     * Provides a connection to the database.
     */
    public static Connection getConnection() {
        try {
            // Register the PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Error connecting to the database", e);
        }
    }
}