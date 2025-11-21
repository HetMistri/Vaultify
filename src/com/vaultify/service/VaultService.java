package com.vaultify.service;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import com.vaultify.crypto.HashUtil;
import com.vaultify.dao.FileCredentialDAO;
import com.vaultify.models.CredentialMetadata;
import com.vaultify.threading.ThreadManager;
import com.vaultify.util.CredentialFileManager;

/**
 * VaultService - REAL IMPLEMENTATION
 * 
 * Core credential vaulting operations:
 * - Encrypt and store files
 * - List user's credentials
 * - Retrieve (decrypt) files
 * - Delete credentials
 * 
 * Uses CredentialFileManager for encryption and FileCredentialDAO for metadata
 * persistence.
 */
public class VaultService {
    private final LedgerService ledgerService;
    private final FileCredentialDAO credentialDAO;

    public VaultService() {
        this.ledgerService = new LedgerService();
        this.credentialDAO = new FileCredentialDAO();
    }

    public VaultService(LedgerService ledgerService, FileCredentialDAO credentialDAO) {
        this.ledgerService = ledgerService;
        this.credentialDAO = credentialDAO;
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

        // 2. Save metadata to DAO
        credentialDAO.save(meta);

        // 3. Append to ledger asynchronously
        String dataHash = HashUtil.sha256(meta.credentialIdString + ":" + meta.dataHash);
        ThreadManager.runAsync(() -> ledgerService.appendBlock("ADD_CREDENTIAL", dataHash));

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
        List<CredentialMetadata> all = credentialDAO.findAll();
        // Filter by userId
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
        // 1. Load metadata
        CredentialMetadata meta = credentialDAO.findById(credentialId);
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
        CredentialMetadata meta = credentialDAO.findById(credentialId);
        if (meta == null) {
            throw new IllegalArgumentException("Credential not found: " + credentialId);
        }

        // 2. Authorization check
        if (meta.userId != userId) {
            throw new SecurityException("Unauthorized: credential belongs to different user");
        }

        // 3. Delete encrypted file
        CredentialFileManager.deleteEncryptedFile(credentialId);

        // 4. Delete metadata
        credentialDAO.delete(credentialId);

        // 5. Append to ledger
        String dataHash = HashUtil.sha256("DELETE:" + credentialId);
        ThreadManager.runAsync(() -> ledgerService.appendBlock("DELETE_CREDENTIAL", dataHash));

        System.out.println("✓ Credential deleted: " + credentialId);
    }
}
