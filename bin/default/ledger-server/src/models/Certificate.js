/**
 * Certificate Model
 * Represents a shareable certificate with signature and public key
 */
export class Certificate {
  constructor(certificateId, payload, signature, issuerPublicKey) {
    this.certificateId = certificateId;
    this.payload = payload; // { issuerUserId, credentialId, tokenHash, expiry, ledgerBlockHash }
    this.signature = signature;
    this.issuerPublicKey = issuerPublicKey;
    this.createdAt = Date.now();
  }

  /**
   * Check if certificate is expired
   * @returns {boolean} True if expired
   */
  isExpired() {
    return Date.now() > this.payload.expiry;
  }

  /**
   * Convert certificate to JSON
   * @returns {Object} JSON representation
   */
  toJSON() {
    return {
      certificateId: this.certificateId,
      payload: this.payload,
      signature: this.signature,
      issuerPublicKey: this.issuerPublicKey,
      createdAt: this.createdAt,
    };
  }

  /**
   * Create certificate from JSON
   * @param {Object} json - JSON object
   * @returns {Certificate} Certificate instance
   */
  static fromJSON(json) {
    const cert = new Certificate(
      json.certificateId,
      json.payload,
      json.signature,
      json.issuerPublicKey
    );
    cert.createdAt = json.createdAt;
    return cert;
  }
}
