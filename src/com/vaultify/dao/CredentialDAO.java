package com.vaultify.dao;

import com.vaultify.db.Database;
import com.vaultify.models.Credential; // You must create this model class
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CredentialDAO {

    /**
     * Implements insertCredential skeleton [cite: 225]
     */
    public void insertCredential(Credential credential) {
        String sql = "INSERT INTO credentials (user_id, filename, filepath, metadata) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, credential.getUserId());
            pstmt.setString(2, credential.getFilename());
            pstmt.setString(3, credential.getFilepath());
            pstmt.setString(4, credential.getMetadata());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting credential", e);
        }
    }

    /**
     * Implements getCredentialsByUser skeleton [cite: 226]
     */
    public List<Credential> getCredentialsByUser(long userId) {
        List<Credential> credentials = new ArrayList<>();
        String sql = "SELECT * FROM credentials WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Credential cred = new Credential();
                    cred.setId(rs.getLong("id"));
                    cred.setUserId(rs.getLong("user_id"));
                    cred.setFilename(rs.getString("filename"));
                    cred.setFilepath(rs.getString("filepath"));
                    cred.setMetadata(rs.getString("metadata"));
                    cred.setCreatedAt(rs.getTimestamp("created_at"));
                    credentials.add(cred);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error getting credentials by user", e);
        }
        return credentials;
    }

    /**
     * Implements deleteCredential skeleton [cite: 227]
     */
    public void deleteCredential(long credentialId) {
        String sql = "DELETE FROM credentials WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, credentialId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting credential", e);
        }
    }
}