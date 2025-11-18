package com.vaultify.dao;

import com.vaultify.db.Database;
import com.vaultify.models.User; // You must create this model class
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /**
     * Implements saveUser skeleton [cite: 220]
     */
    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, password_hash, public_key, private_key_encrypted) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPublicKey());
            pstmt.setString(4, user.getPrivateKeyEncrypted());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    /**
     * Implements findUserByUsername skeleton [cite: 221]
     */
    public User findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        User user = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setPublicKey(rs.getString("public_key"));
                    user.setPrivateKeyEncrypted(rs.getString("private_key_encrypted"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username", e);
        }
        return user;
    }
}