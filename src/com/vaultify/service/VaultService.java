package com.vaultify.service;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import com.vaultify.crypto.HashUtil;
import com.vaultify.dao.FileCredentialDAO;
import com.vaultify.dao.JdbcCredentialDAO;
import com.vaultify.models.CredentialMetadata;
import com.vaultify.threading.ThreadManager;
import com.vaultify.util.CredentialFileManager;

/**
 * VaultService - REAL IMPLEMENTATION with DUAL STORAGE
 * 
 * Core credential vaulting operations:
 * - Encrypt and store files
 * - List user's credentials
 * - Retrieve (decrypt) files
 * - Delete credentials
 * 
 * Uses DUAL storage: File-based for encrypted data + JDBC for metadata
 * querying.
 * CredentialFileManager handles encryption, both DAOs persist metadata.
 */
public class VaultService {
    private final LedgerService ledgerService;
    private final FileCredentialDAO fileCredentialDAO;
    private final JdbcCredentialDAO jdbcCredentialDAO;

    public VaultService() {
        this.ledgerService = new LedgerService();
        this.fileCredentialDAO = new FileCredentialDAO();
        this.jdbcCredentialDAO = new JdbcCredentialDAO();
    }

    public VaultService(LedgerService ledgerService, FileCredentialDAO fileDAO, JdbcCredentialDAO jdbcDAO) {
        this.ledgerService = ledgerService;
        this.fileCredentialDAO = fileDAO;
        this.jdbcCredentialDAO = jdbcDAO;
    }

    /**
     * Add a credential - REAL implementation.
     * 
     * @param userId        Owner user ID
     * @param filePath      Path to file to encrypt and store
     * @param userPublicKey User's RSA public key for key wrapping
     * @return Credential ID (UUID)
     */
    public String addCredential(long userId, Path filePath, PublicKey userPublicKey) throws Exception {
        // 1. Encrypt file and generate metadata
        CredentialMetadata meta = CredentialFileManager.encryptAndStore(filePath, userPublicKey, userId);

        // 2. Save metadata to BOTH storage backends
        fileCredentialDAO.save(meta); // JSON file
        try {
            long generatedId = jdbcCredentialDAO.save(meta, userId); // PostgreSQL database; sets meta.id
            System.out.println("[Dual Storage] Credential metadata saved to both File and Database");
        } catch (Exception e) {
            System.err.println("[Warning] Failed to save credential to database: " + e.getMessage());
        }

        // 3. Append to ledger asynchronously
        String dataHash = HashUtil.sha256(meta.credentialIdString + ":" + meta.dataHash);
        ThreadManager.runAsync(() -> ledgerService.appendBlock(userId, "user_" + userId, "ADD_CREDENTIAL", dataHash));

        System.out.println("✓ Credential added: " + meta.filename + " (ID: " + meta.credentialIdString + ")");
        return meta.credentialIdString;
    }

    /**
     * List all credentials for a user.
     * 
     * @param userId User ID to list credentials for
     * @return List of credential metadata
     */
    public List<CredentialMetadata> listCredentials(long userId) {
        // Try JDBC first (faster with indexed queries)
        try {
            List<CredentialMetadata> dbList = jdbcCredentialDAO.findByUserId(userId);
            if (!dbList.isEmpty()) {
                System.out.println("[Dual Storage] Credentials loaded from Database");
                return dbList;
            }
        } catch (Exception e) {
            System.out.println("[Dual Storage] Database unavailable, using File storage");
        }

        // Fallback to file storage
        List<CredentialMetadata> all = fileCredentialDAO.findAll();
        return all.stream()
                .filter(c -> c.userId == userId)
                .toList();
    }

    /**
     * Retrieve (decrypt) a credential file.
     * 
     * @param credentialId   UUID of the credential
     * @param userPrivateKey User's RSA private key for key unwrapping
     * @return Decrypted file contents
     */
    public byte[] retrieveCredential(String credentialId, PrivateKey userPrivateKey) throws Exception {
        // 1. Load metadata (try JDBC first, fallback to File)
        CredentialMetadata meta = null;
        try {
            // Try database first (load all credentials for user-independent search)
            List<CredentialMetadata> allFromDb = jdbcCredentialDAO.findByUserId(0); // NOTE: credential table lacks
                                                                                    // direct UUID lookup; using legacy
                                                                                    // approach
            meta = allFromDb.stream()
                    .filter(c -> credentialId.equals(c.credentialIdString))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            // Database unavailable, use file
        }

        if (meta == null) {
            meta = fileCredentialDAO.findById(credentialId);
        }

        if (meta == null) {
            throw new IllegalArgumentException("Credential not found: " + credentialId);
        }

        // 2. Decrypt file
        byte[] plaintext = CredentialFileManager.decryptAndRetrieve(
                credentialId,
                meta.encryptedKeyBase64,
                meta.ivBase64,
                userPrivateKey);

        System.out.println("✓ Credential retrieved: " + meta.filename);
        return plaintext;
    }

    /**
     * Delete a credential.
     * 
     * @param credentialId UUID of the credential
     * @param userId       User ID (for authorization check)
     */
    public void deleteCredential(String credentialId, long userId) throws Exception {
        // 1. Load metadata
        CredentialMetadata meta = fileCredentialDAO.findById(credentialId);
        if (meta == null) {
            throw new IllegalArgumentException("Credential not found: " + credentialId);
        }

        // 2. Authorization check
        if (meta.userId != userId) {
            throw new SecurityException("Unauthorized: credential belongs to different user");
        }

        // 3. Delete encrypted file
        CredentialFileManager.deleteEncryptedFile(credentialId);

        // 4. Delete metadata from BOTH storages
        fileCredentialDAO.delete(credentialId);
        try {
            jdbcCredentialDAO.delete(credentialId);
            System.out.println("[Dual Storage] Credential deleted from both File and Database");
        } catch (Exception e) {
            System.err.println("[Warning] Failed to delete from database: " + e.getMessage());
        }

        // 5. Append to ledger
        String dataHash = HashUtil.sha256("DELETE:" + credentialId);
        ThreadManager
                .runAsync(() -> ledgerService.appendBlock(userId, "user_" + userId, "DELETE_CREDENTIAL", dataHash));

        System.out.println("✓ Credential deleted: " + credentialId);
    }
}
