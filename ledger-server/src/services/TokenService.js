import { readJSON, writeJSON } from "../utils/storage.js";

const REVOKED_TOKENS_FILE = "revoked-tokens.json";
const PUBLIC_KEYS_FILE = "public-keys.json";

/**
 * Token Service
 * Manages token revocation and public keys
 */
export class TokenService {
  constructor() {
    this.revokedTokens = this.loadRevokedTokens();
    this.publicKeys = this.loadPublicKeys();
  }

  /**
   * Load revoked tokens from storage
   * @returns {Map<string, Object>} Map of token hash to revocation info
   */
  loadRevokedTokens() {
    const tokensData = readJSON(REVOKED_TOKENS_FILE, []);
    const tokensMap = new Map();

    tokensData.forEach((tokenInfo) => {
      tokensMap.set(tokenInfo.tokenHash, tokenInfo);
    });

    return tokensMap;
  }

  /**
   * Save revoked tokens to storage
   */
  saveRevokedTokens() {
    const tokensArray = Array.from(this.revokedTokens.values());
    writeJSON(REVOKED_TOKENS_FILE, tokensArray);
  }

  /**
   * Load public keys from storage
   * @returns {Map<string, string>} Map of user ID to public key
   */
  loadPublicKeys() {
    const keysData = readJSON(PUBLIC_KEYS_FILE, {});
    return new Map(Object.entries(keysData));
  }

  /**
   * Save public keys to storage
   */
  savePublicKeys() {
    const keysObject = Object.fromEntries(this.publicKeys);
    writeJSON(PUBLIC_KEYS_FILE, keysObject);
  }

  /**
   * Revoke a token
   * @param {string} tokenHash - SHA-256 hash of token
   * @param {string} reason - Reason for revocation
   * @returns {Object} Revocation info
   */
  revokeToken(tokenHash, reason = "No reason provided") {
    if (this.revokedTokens.has(tokenHash)) {
      throw new Error("Token already revoked");
    }

    const revocationInfo = {
      tokenHash,
      reason,
      revokedAt: Date.now(),
    };

    this.revokedTokens.set(tokenHash, revocationInfo);
    this.saveRevokedTokens();

    return revocationInfo;
  }

  /**
   * Check if token is revoked
   * @param {string} tokenHash - Token hash
   * @returns {boolean} True if revoked
   */
  isTokenRevoked(tokenHash) {
    return this.revokedTokens.has(tokenHash);
  }

  /**
   * Get revocation info
   * @param {string} tokenHash - Token hash
   * @returns {Object|null} Revocation info or null
   */
  getRevocationInfo(tokenHash) {
    return this.revokedTokens.get(tokenHash) || null;
  }

  /**
   * Get all revoked tokens
   * @returns {Object[]} All revocation info
   */
  getAllRevokedTokens() {
    return Array.from(this.revokedTokens.values());
  }

  /**
   * Register a user's public key
   * @param {string} userId - User ID
   * @param {string} publicKey - PEM-formatted public key
   * @returns {boolean} Success status
   */
  registerPublicKey(userId, publicKey) {
    this.publicKeys.set(userId, publicKey);
    this.savePublicKeys();
    return true;
  }

  /**
   * Get user's public key
   * @param {string} userId - User ID
   * @returns {string|null} Public key or null
   */
  getPublicKey(userId) {
    return this.publicKeys.get(userId) || null;
  }

  /**
   * Get all public keys
   * @returns {Object} Map of user IDs to public keys
   */
  getAllPublicKeys() {
    return Object.fromEntries(this.publicKeys);
  }

  /**
   * Get token statistics
   * @returns {Object} Statistics
   */
  getStats() {
    return {
      totalRevoked: this.revokedTokens.size,
      totalPublicKeys: this.publicKeys.size,
    };
  }
}

// Singleton instance
export const tokenService = new TokenService();
