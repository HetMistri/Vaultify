package com.vaultify.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.vaultify.db.Database;
import com.vaultify.models.Token;

public class JdbcTokenDAO {

    public void save(Token token) {
        String sql = "INSERT INTO tokens (credential_id, issuer_user_id, token, expiry, revoked) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, token.getCredentialId());
            pstmt.setLong(2, token.getIssuerUserId());
            pstmt.setString(3, token.getToken());
            pstmt.setTimestamp(4, token.getExpiry());
            pstmt.setBoolean(5, token.isRevoked());

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
                    t.setIssuerUserId(rs.getLong("issuer_user_id"));
                    t.setToken(rs.getString("token"));
                    t.setExpiry(rs.getTimestamp("expiry"));
                    t.setRevoked(rs.getBoolean("revoked"));
                    t.setCreatedAt(rs.getTimestamp("created_at"));
                    return t;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while finding token: " + e.getMessage());
        }
        return null;
    }

    public java.util.List<Token> findByUserId(long userId) {
        java.util.List<Token> tokens = new java.util.ArrayList<>();
        String sql = "SELECT * FROM tokens WHERE issuer_user_id = ? ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Token t = new Token();
                    t.setId(rs.getLong("id"));
                    t.setCredentialId(rs.getLong("credential_id"));
                    t.setIssuerUserId(rs.getLong("issuer_user_id"));
                    t.setToken(rs.getString("token"));
                    t.setExpiry(rs.getTimestamp("expiry"));
                    t.setRevoked(rs.getBoolean("revoked"));
                    t.setCreatedAt(rs.getTimestamp("created_at"));
                    tokens.add(t);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while finding user tokens: " + e.getMessage());
        }
        return tokens;
    }

    public void revokeToken(String tokenString) {
        String sql = "UPDATE tokens SET revoked = TRUE WHERE token = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tokenString);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error revoking token in DB", e);
        }
    }

    public int deleteExpiredTokens() {
        String sql = "DELETE FROM tokens WHERE expiry < CURRENT_TIMESTAMP AND revoked = FALSE";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting expired tokens", e);
        }
    }
}