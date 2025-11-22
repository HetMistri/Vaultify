import { tokenService } from "../services/TokenService.js";
import { ledgerService } from "../services/LedgerService.js";
import { sha256 } from "../utils/crypto.js";

/**
 * Token Controller
 * Handles HTTP requests for token operations
 */

/**
 * Revoke a token
 * POST /api/tokens/revoke
 */
export const revokeToken = (req, res) => {
  try {
    const { tokenHash, reason } = req.body;

    // Validate input
    if (!tokenHash) {
      return res.status(400).json({ error: "Missing tokenHash" });
    }

    // Revoke token
    const revocationInfo = tokenService.revokeToken(tokenHash, reason);

    // Append to ledger
    const block = ledgerService.appendBlock("REVOKE_TOKEN", tokenHash);

    res.status(201).json({
      message: "Token revoked successfully",
      revocationInfo,
      ledgerBlock: block.toJSON(),
    });
  } catch (error) {
    console.error("Error revoking token:", error);
    res.status(400).json({ error: error.message });
  }
};

/**
 * Check if token is revoked
 * GET /api/tokens/revoked/:tokenHash
 */
export const checkRevocation = (req, res) => {
  try {
    const { tokenHash } = req.params;
    const isRevoked = tokenService.isTokenRevoked(tokenHash);
    const revocationInfo = isRevoked
      ? tokenService.getRevocationInfo(tokenHash)
      : null;

    res.json({
      tokenHash,
      isRevoked,
      revocationInfo,
    });
  } catch (error) {
    console.error("Error checking revocation:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get all revoked tokens
 * GET /api/tokens/revoked
 */
export const getAllRevokedTokens = (req, res) => {
  try {
    const revokedTokens = tokenService.getAllRevokedTokens();
    res.json({
      total: revokedTokens.length,
      revokedTokens,
    });
  } catch (error) {
    console.error("Error getting revoked tokens:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Register a user's public key
 * POST /api/users/:userId/public-key
 */
export const registerPublicKey = (req, res) => {
  try {
    const { userId } = req.params;
    const { publicKey } = req.body;

    // Validate input
    if (!publicKey) {
      return res.status(400).json({ error: "Missing publicKey" });
    }

    // Register public key
    tokenService.registerPublicKey(userId, publicKey);

    // Append to ledger
    const publicKeyHash = sha256(publicKey);
    const block = ledgerService.appendBlock(
      "REGISTER_PUBLIC_KEY",
      publicKeyHash
    );

    res.status(201).json({
      message: "Public key registered successfully",
      userId,
      ledgerBlock: block.toJSON(),
    });
  } catch (error) {
    console.error("Error registering public key:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get user's public key
 * GET /api/users/:userId/public-key
 */
export const getPublicKey = (req, res) => {
  try {
    const { userId } = req.params;
    const publicKey = tokenService.getPublicKey(userId);

    if (!publicKey) {
      return res.status(404).json({ error: "Public key not found" });
    }

    res.json({ userId, publicKey });
  } catch (error) {
    console.error("Error getting public key:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get all public keys
 * GET /api/users/public-keys
 */
export const getAllPublicKeys = (req, res) => {
  try {
    const publicKeys = tokenService.getAllPublicKeys();
    res.json({
      total: Object.keys(publicKeys).length,
      publicKeys,
    });
  } catch (error) {
    console.error("Error getting public keys:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get token statistics
 * GET /api/tokens/stats
 */
export const getTokenStats = (req, res) => {
  try {
    const stats = tokenService.getStats();
    res.json(stats);
  } catch (error) {
    console.error("Error getting stats:", error);
    res.status(500).json({ error: error.message });
  }
};
