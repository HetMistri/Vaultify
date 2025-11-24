package com.vaultify.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.vaultify.db.Database;
import com.vaultify.models.CredentialMetadata;
import com.vaultify.models.CredentialType;

/**
 * JDBC-backed implementation of CredentialRepository.
 * Maps CredentialMetadata onto credentials table columns.
 */
public class PostgresCredentialRepository implements CredentialRepository {

    @Override
    public long save(CredentialMetadata meta, long userId) {
        // Serialize encryption-related fields into metadata JSON for full persistence
        String metadataJson = toMetadataJson(meta);
        String sql = "INSERT INTO credentials (user_id, filename, filepath, metadata, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, meta.filename);
            ps.setString(3, "vault_data/credentials/" + meta.credentialIdString + ".bin");
            ps.setString(4, metadataJson);
            ps.setTimestamp(5, new Timestamp(meta.timestamp));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    meta.id = (int) id;
                    return id;
                }
            }
            return -1L;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save credential metadata", e);
        }
    }

    @Override
    public CredentialMetadata findByCredentialId(String credentialId) {
        String sql = "SELECT * FROM credentials WHERE filepath = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "vault_data/credentials/" + credentialId + ".bin");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CredentialMetadata meta = hydrate(rs);
                    meta.credentialIdString = credentialId; // override derived id
                    return meta;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Failed findByCredentialId", e);
        }
    }

    @Override
    public List<CredentialMetadata> findByUserId(long userId) {
        List<CredentialMetadata> list = new ArrayList<>();
        String sql = "SELECT * FROM credentials WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CredentialMetadata meta = hydrate(rs);
                    list.add(meta);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed findByUserId", e);
        }
        return list;
    }

    @Override
    public void deleteByCredentialId(String credentialId) {
        String sql = "DELETE FROM credentials WHERE filepath = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "vault_data/credentials/" + credentialId + ".bin");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Failed deleteByCredentialId", e);
        }
    }

    private CredentialMetadata hydrate(ResultSet rs) throws SQLException {
        CredentialMetadata meta = new CredentialMetadata();
        meta.id = rs.getInt("id");
        meta.userId = rs.getLong("user_id");
        meta.filename = rs.getString("filename");
        meta.type = CredentialType.FILE;
        meta.timestamp = rs.getTimestamp("created_at").getTime();
        String path = rs.getString("filepath");
        String filename = new java.io.File(path).getName();
        meta.credentialIdString = filename.replace(".bin", "");
        String metadataJson = rs.getString("metadata");
        if (metadataJson != null && !metadataJson.isBlank()) {
            fromMetadataJson(metadataJson, meta);
        }
        return meta;
    }

    private String toMetadataJson(CredentialMetadata meta) {
        // Minimal manual JSON to avoid external libs
        StringBuilder sb = new StringBuilder("{");
        appendJson(sb, "encryptedKeyBase64", meta.encryptedKeyBase64);
        sb.append(',');
        appendJson(sb, "ivBase64", meta.ivBase64);
        sb.append(',');
        appendJson(sb, "dataHash", meta.dataHash);
        sb.append(',');
        sb.append("\"fileSize\":").append(meta.fileSize);
        sb.append('}');
        return sb.toString();
    }

    private void appendJson(StringBuilder sb, String key, String val) {
        sb.append('"').append(key).append('"').append(':');
        if (val == null)
            sb.append("null");
        else
            sb.append('"').append(val.replace("\"", "\\\"")).append('"');
    }

    private void fromMetadataJson(String json, CredentialMetadata meta) {
        // Very lightweight parsing assuming flat object with quoted string values and
        // fileSize number
        // Not robust for arbitrary JSON; sufficient for controlled writes.
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1); // remove braces
                String[] parts = trimmed.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":", 2);
                    if (kv.length != 2)
                        continue;
                    String key = kv[0].trim().replaceAll("^\"|\"$", "");
                    String valueRaw = kv[1].trim();
                    if ("fileSize".equals(key)) {
                        try {
                            meta.fileSize = Long.parseLong(valueRaw.replaceAll("[^0-9]", ""));
                        } catch (NumberFormatException ignored) {
                        }
                        continue;
                    }
                    if (valueRaw.equals("null")) {
                        continue;
                    }
                    String value = valueRaw.replaceAll("^\"|\"$", "");
                    switch (key) {
                        case "encryptedKeyBase64" -> meta.encryptedKeyBase64 = value;
                        case "ivBase64" -> meta.ivBase64 = value;
                        case "dataHash" -> meta.dataHash = value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
