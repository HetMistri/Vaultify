package com.vaultify.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.vaultify.db.Database;
import com.vaultify.models.User;

public class JdbcUserDAO {

    public void save(User user) {
        String sql = "INSERT INTO users (username, password_hash, public_key, private_key_encrypted, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPublicKey());
            pstmt.setString(4, user.getPrivateKeyEncrypted());
            pstmt.setTimestamp(5, user.getCreatedAt());

            pstmt.executeUpdate();

            // Optional: Retrieve generated ID and set it back to user object
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error saving user to DB", e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setPublicKey(rs.getString("public_key"));
                    user.setPrivateKeyEncrypted(rs.getString("private_key_encrypted"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while finding user: " + e.getMessage());
        }
        return null; // Not found
    }
}