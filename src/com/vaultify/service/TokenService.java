package com.vaultify.service;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import com.vaultify.crypto.HashUtil;
import com.vaultify.util.TokenUtil;
import com.vaultify.verifier.Certificate;
import com.vaultify.verifier.CertificateParser;

/**
 * TokenService - Token generation, validation, and certificate creation.
 * 
 * Handles:
 * - Generating unique share tokens
 * - Creating signed certificates for tokens
 * - Validating token format
 * - Revoking tokens (simple in-memory set)
 */
public class TokenService {
    private final LedgerService ledgerService;
    private final Set<String> revokedTokens; // Simple revocation list

    public TokenService() {
        this.ledgerService = new LedgerService();
        this.revokedTokens = new HashSet<>();
    }

    /**
     * Generate a unique share token.
     * 
     * @return 32-character hex token
     */
    public String generateToken() {
        return TokenUtil.generateToken();
    }

    /**
     * Create a signed certificate for a token.
     * 
     * @param token            The share token
     * @param issuerUserId     User ID of issuer
     * @param credentialId     Credential being shared
     * @param expiryHours      Token validity in hours
     * @param issuerPrivateKey Issuer's RSA private key for signing
     * @param outputPath       Where to save certificate JSON
     * @return Created certificate
     */
    public Certificate createCertificate(String token, long issuerUserId, long credentialId,
            int expiryHours, PrivateKey issuerPrivateKey,
            Path outputPath) throws Exception {
        long now = System.currentTimeMillis();
        long expiry = now + (expiryHours * 3600L * 1000L);

        // Create payload hash: sha256(token|issuer|credential|expiry)
        String payload = token + "|" + issuerUserId + "|" + credentialId + "|" + expiry;
        String payloadHash = HashUtil.sha256(payload);

        // Sign the payload hash with issuer's private key
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(issuerPrivateKey);
        sig.update(payloadHash.getBytes());
        byte[] signature = sig.sign();
        String signatureBase64 = Base64.getEncoder().encodeToString(signature);

        // Append to ledger and get block hash
        String ledgerBlockHash = ledgerService.appendBlock("GENERATE_TOKEN", payloadHash).getHash();

        // Create certificate
        Certificate cert = new Certificate();
        cert.token = token;
        cert.issuerUserId = issuerUserId;
        cert.credentialId = credentialId;
        cert.expiryEpochMs = expiry;
        cert.payloadHash = payloadHash;
        cert.signatureBase64 = signatureBase64;
        cert.ledgerBlockHash = ledgerBlockHash;
        cert.createdAtMs = now;

        // Save to file
        CertificateParser.save(cert, outputPath);

        System.out.println("✓ Certificate created: " + outputPath);
        System.out.println("  Token: " + token);
        System.out.println("  Expires: " + new java.util.Date(expiry));

        return cert;
    }

    /**
     * Validate token format (basic check).
     * 
     * @param token Token to validate
     * @return true if format is valid
     */
    public boolean validateToken(String token) {
        if (token == null || token.length() != 32) {
            return false;
        }

        // Check if revoked
        if (revokedTokens.contains(token)) {
            System.out.println("✗ Token has been revoked");
            return false;
        }

        // Check hex format
        return token.matches("[0-9a-f]{32}");
    }

    /**
     * Revoke a token (add to revocation list).
     * 
     * @param token Token to revoke
     */
    public void revokeToken(String token) {
        revokedTokens.add(token);
        System.out.println("✓ Token revoked: " + token);
    }

    /**
     * Check if token is revoked.
     */
    public boolean isRevoked(String token) {
        return revokedTokens.contains(token);
    }
}
