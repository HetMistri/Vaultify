package com.vaultify.dao;

import com.vaultify.db.Database;
import com.vaultify.models.CredentialMetadata;
import com.vaultify.models.CredentialType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcCredentialDAO {

    public void save(CredentialMetadata meta, long userId) {
        String sql = "INSERT INTO credentials (user_id, filename, filepath, metadata, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, meta.filename);
            // Store the relative path to the encrypted bin file
            pstmt.setString(3, "vault_data/credentials/" + meta.credentialIdString + ".bin");
            // Store the rest of the metadata (hash, size) as a JSON string or text
            pstmt.setString(4, "hash:" + meta.dataHash + ";size:" + meta.fileSize);
            pstmt.setTimestamp(5, new Timestamp(meta.timestamp));

            pstmt.executeUpdate();

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
                    // Extract ID from filepath for consistency with FileDAO
                    String path = rs.getString("filepath");
                    // e.g. "vault_data/credentials/abc-123.bin" -> "abc-123"
                    String filename = new java.io.File(path).getName();
                    meta.credentialIdString = filename.replace(".bin", "");

                    meta.filename = rs.getString("filename");
                    meta.timestamp = rs.getTimestamp("created_at").getTime();
                    meta.type = CredentialType.FILE;

                    // Parse simple metadata string "hash:xyz;size:123"
                    String metaStr = rs.getString("metadata");
                    if (metaStr != null) {
                        String[] parts = metaStr.split(";");
                        for (String p : parts) {
                            if (p.startsWith("size:")) meta.fileSize = Long.parseLong(p.split(":")[1]);
                            if (p.startsWith("hash:")) meta.dataHash = p.split(":")[1];
                        }
                    }
                    list.add(meta);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}