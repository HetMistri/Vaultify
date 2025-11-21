package com.vaultify.verifier;

import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.vaultify.crypto.KeyManager;
import com.vaultify.ledger.LedgerBlock;
import com.vaultify.service.LedgerService;

public class CertificateVerifier {

    /**
     * Verify the certificate:
     * 1) Verify signature using issuer public key (PEM path)
     * 2) Confirm ledger contains a block whose dataHash equals payloadHash and
     * whose block.hash equals ledgerBlockHash
     * 3) Check expiry
     *
     * Returns a Result object describing validity and reason.
     */
    public static Result verify(Certificate cert, Path issuerPublicKeyPath, LedgerService ledgerService) {
        try {
            // 1) verify signature over payloadHash
            PublicKey pub = new KeyManager().loadPublicKey(issuerPublicKeyPath);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            byte[] payloadHashBytes = cert.payloadHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sig.update(payloadHashBytes);
            byte[] signatureBytes = Base64.getDecoder().decode(cert.signatureBase64);
            boolean sigOk = sig.verify(signatureBytes);
            if (!sigOk) {
                return new Result(false, "Invalid RSA signature");
            }

            // 2) check ledger for matching block
            List<LedgerBlock> chain = ledgerService.getChain();
            Optional<LedgerBlock> found = chain.stream()
                    .filter(b -> b.getDataHash() != null && b.getDataHash().equals(cert.payloadHash))
                    .filter(b -> b.getHash() != null && b.getHash().equals(cert.ledgerBlockHash))
                    .findAny();
            if (found.isEmpty()) {
                return new Result(false, "Ledger entry not found or ledgerBlockHash mismatch");
            }

            // 3) expiry
            long now = System.currentTimeMillis();
            if (cert.expiryEpochMs <= now) {
                return new Result(false, "Certificate expired");
            }

            return new Result(true, "Certificate valid");
        } catch (Exception e) {
            return new Result(false, "Verification error: " + e.getMessage());
        }
    }

    public static class Result {
        public final boolean valid;
        public final String message;

        public Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}
