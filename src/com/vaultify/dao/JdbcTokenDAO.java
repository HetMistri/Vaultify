package com.vaultify.dao;

import com.vaultify.db.Database;
import com.vaultify.models.Token;
import java.sql.*;

public class JdbcTokenDAO {

    public void save(Token token) {
        String sql = "INSERT INTO tokens (credential_id, token, expiry) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, token.getCredentialId());
            pstmt.setString(2, token.getToken());
            pstmt.setTimestamp(3, token.getExpiry());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving token to DB", e);
        }
    }

    public Token findByToken(String tokenString) {
        String sql = "SELECT * FROM tokens WHERE token = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tokenString);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Token t = new Token();
                    t.setId(rs.getLong("id"));
                    t.setCredentialId(rs.getLong("credential_id"));
                    t.setToken(rs.getString("token"));
                    t.setExpiry(rs.getTimestamp("expiry"));
                    return t;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}