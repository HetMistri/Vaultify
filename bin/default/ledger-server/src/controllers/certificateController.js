import { certificateService } from "../services/CertificateService.js";
import { ledgerService } from "../services/LedgerService.js";
import { tokenService } from "../services/TokenService.js";
import { sha256 } from "../utils/crypto.js";

/**
 * Certificate Controller
 * Handles HTTP requests for certificate operations
 */

/**
 * Register a new certificate
 * POST /api/certificates
 */
export const registerCertificate = (req, res) => {
  try {
    const { certificateId, payload, signature, issuerPublicKey } = req.body;

    // Validate input
    if (!certificateId || !payload || !signature || !issuerPublicKey) {
      return res.status(400).json({
        error:
          "Missing required fields: certificateId, payload, signature, issuerPublicKey",
      });
    }

    // Validate payload structure
    const { issuerUserId, credentialId, tokenHash, expiry, ledgerBlockHash } =
      payload;
    if (
      !issuerUserId ||
      !credentialId ||
      !tokenHash ||
      !expiry ||
      !ledgerBlockHash
    ) {
      return res.status(400).json({
        error: "Invalid payload structure",
      });
    }

    // Verify ledger block exists
    const block = ledgerService.getBlockByHash(ledgerBlockHash);
    if (!block) {
      return res.status(400).json({
        error: "Ledger block not found",
      });
    }

    // Register certificate
    const certificate = certificateService.registerCertificate(
      certificateId,
      payload,
      signature,
      issuerPublicKey
    );

    res.status(201).json({
      message: "Certificate registered successfully",
      certificate: certificate.toJSON(),
    });
  } catch (error) {
    console.error("Error registering certificate:", error);
    res.status(400).json({ error: error.message });
  }
};

/**
 * Get certificate by ID
 * GET /api/certificates/:certificateId
 */
export const getCertificate = (req, res) => {
  try {
    const { certificateId } = req.params;
    const certificate = certificateService.getCertificate(certificateId);

    if (!certificate) {
      return res.status(404).json({ error: "Certificate not found" });
    }

    res.json(certificate.toJSON());
  } catch (error) {
    console.error("Error getting certificate:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get all certificates
 * GET /api/certificates
 */
export const getAllCertificates = (req, res) => {
  try {
    const certificates = certificateService.getAllCertificates();
    res.json({
      total: certificates.length,
      certificates: certificates.map((c) => c.toJSON()),
    });
  } catch (error) {
    console.error("Error getting certificates:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Verify certificate with token
 * POST /api/certificates/:certificateId/verify
 */
export const verifyCertificate = (req, res) => {
  try {
    const { certificateId } = req.params;
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({ error: "Missing token" });
    }

    // Basic certificate verification
    const basicResult = certificateService.verifyCertificate(
      certificateId,
      token
    );

    if (!basicResult.valid) {
      return res.json(basicResult);
    }

    const certificate = basicResult.certificate;

    // Check token revocation
    const tokenHash = sha256(token);
    const isRevoked = tokenService.isTokenRevoked(tokenHash);

    if (isRevoked) {
      const revocationInfo = tokenService.getRevocationInfo(tokenHash);
      return res.json({
        valid: false,
        reason: "Token has been revoked",
        revocationInfo,
      });
    }

    // Verify ledger block exists and is in chain
    const block = ledgerService.getBlockByHash(
      certificate.payload.ledgerBlockHash
    );
    if (!block) {
      return res.json({
        valid: false,
        reason: "Ledger block not found",
      });
    }

    // Full verification passed
    res.json({
      valid: true,
      certificate: certificate.toJSON(),
      block: block.toJSON(),
    });
  } catch (error) {
    console.error("Error verifying certificate:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get certificates by issuer
 * GET /api/certificates/issuer/:userId
 */
export const getCertificatesByIssuer = (req, res) => {
  try {
    const { userId } = req.params;
    const certificates = certificateService.getCertificatesByIssuer(userId);

    res.json({
      total: certificates.length,
      certificates: certificates.map((c) => c.toJSON()),
    });
  } catch (error) {
    console.error("Error getting certificates:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get certificate statistics
 * GET /api/certificates/stats
 */
export const getCertificateStats = (req, res) => {
  try {
    const stats = certificateService.getStats();
    res.json(stats);
  } catch (error) {
    console.error("Error getting stats:", error);
    res.status(500).json({ error: error.message });
  }
};
