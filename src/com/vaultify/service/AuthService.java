package com.vaultify.service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import com.vaultify.crypto.AESEngine;
import com.vaultify.crypto.HashUtil;
import com.vaultify.crypto.RSAEngine;
import com.vaultify.dao.FileUserDAO;
import com.vaultify.models.User;

/**
 * AuthService handles authentication operations: login, registration, session
 * management.
 * Uses crypto for password hashing, RSA key generation, and AES private key
 * encryption.
 */
public class AuthService {
    private final FileUserDAO userDAO;

    // Current session
    private User currentUser;
    private PrivateKey currentUserPrivateKey;

    public AuthService() {
        this.userDAO = new FileUserDAO();
    }

    /**
     * Register a new user with username and password.
     * Generates RSA key pair, encrypts private key with password-derived AES key.
     * 
     * @param username Username for the new user
     * @param password Plain text password
     * @return User object if successful, null if username exists
     */
    public User register(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        // Check if user already exists
        if (userDAO.findByUsername(username) != null) {
            return null; // Username already taken
        }

        try {
            // Create new user
            User user = new User();
            user.setUsername(username);

            // Hash password with SHA-256
            String passwordHash = HashUtil.sha256(password);
            user.setPasswordHash(passwordHash);

            // Generate RSA key pair (2048-bit)
            KeyPair keyPair = RSAEngine.generateKeyPair(2048);

            // Store public key as Base64-encoded X.509 format
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            user.setPublicKey(publicKeyBase64);

            // Encrypt private key with AES using password-derived key
            // Use first 32 bytes of password hash as AES key
            byte[] aesKey = Base64.getDecoder().decode(Base64.getEncoder().encodeToString(
                    HashUtil.sha256(password).substring(0, 32).getBytes()));
            if (aesKey.length < 32) {
                // Ensure 32 bytes for AES-256
                aesKey = HashUtil.sha256(password).substring(0, 32).getBytes();
                if (aesKey.length < 32) {
                    byte[] padded = new byte[32];
                    System.arraycopy(aesKey, 0, padded, 0, aesKey.length);
                    aesKey = padded;
                }
            }

            byte[] iv = AESEngine.generateIv();
            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            byte[] encryptedPrivateKey = AESEngine.encryptWithParams(privateKeyBytes, aesKey, iv);

            // Store as Base64: IV || encrypted_private_key
            byte[] combined = new byte[iv.length + encryptedPrivateKey.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedPrivateKey, 0, combined, iv.length, encryptedPrivateKey.length);
            String encryptedPrivateKeyBase64 = Base64.getEncoder().encodeToString(combined);
            user.setPrivateKeyEncrypted(encryptedPrivateKeyBase64);

            // Save user to file
            userDAO.save(user);

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to register user: " + e.getMessage(), e);
        }
    }

    /**
     * Login with username and password.
     * Verifies password hash and decrypts private key.
     * 
     * @param username Username
     * @param password Plain text password
     * @return true if login successful, false otherwise
     */
    public boolean login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        try {
            // Find user
            User user = userDAO.findByUsername(username);
            if (user == null) {
                return false;
            }

            // Verify password
            String passwordHash = HashUtil.sha256(password);
            if (!passwordHash.equals(user.getPasswordHash())) {
                return false;
            }

            // Decrypt private key
            byte[] aesKey = HashUtil.sha256(password).substring(0, 32).getBytes();
            if (aesKey.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(aesKey, 0, padded, 0, aesKey.length);
                aesKey = padded;
            }

            byte[] combined = Base64.getDecoder().decode(user.getPrivateKeyEncrypted());
            byte[] iv = new byte[AESEngine.GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, AESEngine.GCM_IV_BYTES);
            byte[] encryptedPrivateKey = new byte[combined.length - AESEngine.GCM_IV_BYTES];
            System.arraycopy(combined, AESEngine.GCM_IV_BYTES, encryptedPrivateKey, 0, encryptedPrivateKey.length);

            byte[] privateKeyBytes = AESEngine.decryptWithParams(encryptedPrivateKey, aesKey, iv);

            // Reconstruct private key
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(
                    privateKeyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Set session
            this.currentUser = user;
            this.currentUserPrivateKey = privateKey;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Logout current user.
     */
    public void logout() {
        this.currentUser = null;
        this.currentUserPrivateKey = null;
    }

    /**
     * Check if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Get current logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get current user's private key (decrypted).
     */
    public PrivateKey getCurrentUserPrivateKey() {
        return currentUserPrivateKey;
    }

    /**
     * Get current user's public key.
     */
    public PublicKey getCurrentUserPublicKey() throws Exception {
        if (currentUser == null) {
            return null;
        }
        byte[] publicKeyBytes = Base64.getDecoder().decode(currentUser.getPublicKey());
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
