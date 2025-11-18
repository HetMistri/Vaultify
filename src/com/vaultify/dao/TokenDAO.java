package com.vaultify.dao;

import com.vaultify.db.Database;
import com.vaultify.models.Token; // You must create this model class
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TokenDAO {

    /**
     * Implements saveToken skeleton [cite: 232]
     */
    public void saveToken(Token token) {
        String sql = "INSERT INTO tokens (credential_id, token, expiry) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, token.getCredentialId());
            pstmt.setString(2, token.getToken());
            pstmt.setTimestamp(3, token.getExpiry());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving token", e);
        }
    }

    /**
     * Implements findToken skeleton [cite: 233]
     */
    public Token findToken(String tokenString) {
        String sql = "SELECT * FROM tokens WHERE token = ?";
        Token token = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tokenString);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    token = new Token();
                    token.setId(rs.getLong("id"));
                    token.setCredentialId(rs.getLong("credential_id"));
                    token.setToken(rs.getString("token"));
                    token.setExpiry(rs.getTimestamp("expiry"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding token", e);
        }
        return token;
    }
}