import express from "express";
import * as ledgerController from "../controllers/ledgerController.js";
import * as certificateController from "../controllers/certificateController.js";
import * as tokenController from "../controllers/tokenController.js";

const router = express.Router();

// ============================================
// LEDGER ROUTES
// ============================================

// Append block
router.post("/ledger/blocks", ledgerController.appendBlock);

// Get block by hash
router.get("/ledger/blocks/:hash", ledgerController.getBlockByHash);

// Get block by index
router.get("/ledger/blocks/index/:index", ledgerController.getBlockByIndex);

// Get all blocks
router.get("/ledger/blocks", ledgerController.getAllBlocks);

// Verify chain integrity
router.get("/ledger/verify", ledgerController.verifyChain);

// Get ledger statistics
router.get("/ledger/stats", ledgerController.getStats);

// ============================================
// CERTIFICATE ROUTES
// ============================================

// Register certificate
router.post("/certificates", certificateController.registerCertificate);

// Get certificate by ID
router.get(
  "/certificates/:certificateId",
  certificateController.getCertificate
);

// Get all certificates
router.get("/certificates", certificateController.getAllCertificates);

// Verify certificate with token
router.post(
  "/certificates/:certificateId/verify",
  certificateController.verifyCertificate
);

// Get certificates by issuer
router.get(
  "/certificates/issuer/:userId",
  certificateController.getCertificatesByIssuer
);

// Get certificate statistics
router.get("/certificates/stats", certificateController.getCertificateStats);

// ============================================
// TOKEN ROUTES
// ============================================

// Revoke token
router.post("/tokens/revoke", tokenController.revokeToken);

// Check if token is revoked
router.get("/tokens/revoked/:tokenHash", tokenController.checkRevocation);

// Get all revoked tokens
router.get("/tokens/revoked", tokenController.getAllRevokedTokens);

// Get token statistics
router.get("/tokens/stats", tokenController.getTokenStats);

// ============================================
// PUBLIC KEY ROUTES
// ============================================

// Register public key
router.post("/users/:userId/public-key", tokenController.registerPublicKey);

// Get user's public key
router.get("/users/:userId/public-key", tokenController.getPublicKey);

// Get all public keys
router.get("/users/public-keys", tokenController.getAllPublicKeys);

// ============================================
// HEALTH CHECK
// ============================================

router.get("/health", (req, res) => {
  res.json({
    status: "OK",
    timestamp: Date.now(),
    service: "Vaultify Ledger Server",
  });
});

export default router;
