package com.vaultify.service;

import com.vaultify.crypto.KeyManager;
import com.vaultify.crypto.RSAEngine;
import com.vaultify.crypto.HashUtil;
import com.vaultify.util.Config;
import com.vaultify.util.TokenUtil;
import com.vaultify.verifier.Certificate;
import com.vaultify.verifier.CertificateParser;
import com.vaultify.verifier.CertificateVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Duration;
import java.util.Base64;

/**
 * VerificationService - generates signed certificates for share tokens and verifies them.
 *
 * NOTE: This implementation expects the issuer's RSA private key to be stored as PKCS#8 PEM
 * at a path the caller provides (or can be derived from user record).
 */
public class VerificationService {
    private final LedgerService ledgerService;

    public VerificationService() {
        this.ledgerService = new LedgerService();
    }

    public VerificationService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * Generate a share token and a signed certificate file on disk.
     *
     * Minimal inputs:
     * - issuerUserId: numeric id for the user issuing the share (informational)
     * - credentialId: id of credential being shared
     * - issuerPrivateKeyPath: path to issuer's PKCS#8 PEM private key (on disk)
     * - expiryHours: how long token is valid
     *
     * Returns path to certificate file (and prints token).
     */
    public Path generateShareToken(long issuerUserId, long credentialId, Path issuerPrivateKeyPath, int expiryHours) throws Exception {
        // create token
        String token = TokenUtil.generateToken();
        long now = System.currentTimeMillis();
        long expiryMs = now + Duration.ofHours(Math.max(1, expiryHours)).toMillis();

        // payload string deterministically
        String payload = token + "|" + issuerUserId + "|" + credentialId + "|" + expiryMs;
        String payloadHash = HashUtil.sha256(payload);

        // sign payloadHash with issuer private key
        KeyManager km = new KeyManager();
        PrivateKey priv = km.loadPrivateKey(issuerPrivateKeyPath);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(priv);
        sig.update(payloadHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] signature = sig.sign();
        String signatureB64 = Base64.getEncoder().encodeToString(signature);

        // append ledger block (synchronous)
        String action = "GENERATE_TOKEN";
        com.vaultify.ledger.LedgerBlock block = ledgerService.getChain().isEmpty()
                ? ledgerService.getChain().get(0) // should never happen
                : ledgerService.getChain().get(ledgerService.getChain().size() - 1);

        // ledgerService.appendBlock returns void in your code; call and then fetch last block
        ledgerService.appendBlock(action, payloadHash);
        // after append, fetch last block
        com.vaultify.ledger.LedgerBlock last = ledgerService.getChain().get(ledgerService.getChain().size() - 1);
        String ledgerHash = last.getHash();

        // Build certificate object
        Certificate cert = new Certificate();
        cert.token = token;
        cert.issuerUserId = issuerUserId;
        cert.credentialId = credentialId;
        cert.expiryEpochMs = expiryMs;
        cert.payloadHash = payloadHash;
        cert.signatureBase64 = signatureB64;
        cert.ledgerBlockHash = ledgerHash;
        cert.createdAtMs = now;

        // Save certificate JSON to disk
        String certificateDir = Config.get("certificate.output", "./vault_data/certificates/");
        Path outDir = Paths.get(certificateDir);
        Files.createDirectories(outDir);
        Path out = outDir.resolve("cert-" + token + ".json");
        CertificateParser.save(cert, out);

        // print essential info
        System.out.println("Generated token: " + token);
        System.out.println("Saved certificate: " + out.toAbsolutePath());

        return out;
    }

    /**
     * Verify certificate at certPath using issuer public key path.
     * Returns CertificateVerifier.Result describing validity and message.
     */
    public CertificateVerifier.Result verifyCertificate(Path certPath, Path issuerPublicKeyPath) throws Exception {
        Certificate cert = CertificateParser.parse(certPath);
        CertificateVerifier.Result res = CertificateVerifier.verify(cert, issuerPublicKeyPath, ledgerService);
        return res;
    }
}
