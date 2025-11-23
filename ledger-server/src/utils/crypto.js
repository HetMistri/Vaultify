import crypto from "crypto";

/**
 * Compute SHA-256 hash of data
 * @param {string} data - Data to hash
 * @returns {string} Hex-encoded hash
 */
export function sha256(data) {
  return crypto.createHash("sha256").update(data).digest("hex");
}

/**
 * Verify RSA signature
 * @param {string} publicKey - PEM-formatted public key
 * @param {string} data - Original data that was signed
 * @param {string} signature - Base64-encoded signature
 * @returns {boolean} True if signature is valid
 */
export function verifySignature(publicKey, data, signature) {
  try {
    const verifier = crypto.createVerify("SHA256");
    verifier.update(data);
    verifier.end();
    return verifier.verify(publicKey, signature, "base64");
  } catch (error) {
    console.error("Signature verification error:", error.message);
    return false;
  }
}

/**
 * Generate a random token (for testing purposes)
 * @param {number} bytes - Number of random bytes
 * @returns {string} Hex-encoded random token
 */
export function generateRandomToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString("hex");
}
