package com.vaultify.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.vaultify.db.Database;
import com.vaultify.models.CredentialMetadata;
import com.vaultify.models.CredentialType;

public class JdbcCredentialDAO {

    public long save(CredentialMetadata meta, long userId) {
        String sql = "INSERT INTO credentials (user_id, filename, filepath, encrypted_key, iv, data_hash, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, meta.filename);
            pstmt.setString(3, "vault_data/credentials/" + meta.credentialIdString + ".bin");
            pstmt.setString(4, meta.encryptedKeyBase64);
            pstmt.setString(5, meta.ivBase64);
            pstmt.setString(6, meta.dataHash);
            pstmt.setLong(7, meta.fileSize);
            pstmt.setTimestamp(8, new Timestamp(meta.timestamp));

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long generatedId = rs.getLong(1);
                    meta.id = (int) generatedId; // store for downstream usage
                    return generatedId;
                }
            }
            return 0L; // fallback if no key (shouldn't happen with SERIAL PK)
        } catch (SQLException e) {
            throw new RuntimeException("Error saving credential to DB", e);
        }
    }

    public List<CredentialMetadata> findByUserId(long userId) {
        List<CredentialMetadata> list = new ArrayList<>();
        String sql = "SELECT * FROM credentials WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CredentialMetadata meta = new CredentialMetadata();
                    meta.id = rs.getInt("id");
                    // Extract ID from filepath for consistency with FileDAO
                    String path = rs.getString("filepath");
                    String filename = new java.io.File(path).getName();
                    meta.credentialIdString = filename.replace(".bin", "");

                    meta.filename = rs.getString("filename");
                    meta.timestamp = rs.getTimestamp("created_at").getTime();
                    meta.type = CredentialType.FILE;
                    meta.userId = userId;

                    // Retrieve encryption fields
                    meta.encryptedKeyBase64 = rs.getString("encrypted_key");
                    meta.ivBase64 = rs.getString("iv");
                    meta.dataHash = rs.getString("data_hash");
                    meta.fileSize = rs.getLong("file_size");

                    list.add(meta);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while finding credentials: " + e.getMessage());
        }
        return list;
    }

    public void delete(String credentialId) {
        String sql = "DELETE FROM credentials WHERE filepath = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "vault_data/credentials/" + credentialId + ".bin");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting credential from DB", e);
        }
    }
}