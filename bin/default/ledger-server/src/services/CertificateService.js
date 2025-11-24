import { Certificate } from "../models/Certificate.js";
import { readJSON, writeJSON, writeJSONAsync } from "../utils/storage.js";
import { verifySignature, sha256 } from "../utils/crypto.js";

const CERTIFICATES_FILE = "certificates.json";

/**
 * Certificate Service
 * Manages certificate registry
 */
export class CertificateService {
  constructor() {
    this.certificates = this.loadCertificates();
  }

  /**
   * Load certificates from storage
   * @returns {Map<string, Certificate>} Map of certificate ID to Certificate
   */
  loadCertificates() {
    const certsData = readJSON(CERTIFICATES_FILE, []);
    const certsMap = new Map();

    certsData.forEach((certData) => {
      const cert = Certificate.fromJSON(certData);
      certsMap.set(cert.certificateId, cert);
    });

    return certsMap;
  }

  /**
   * Save certificates to storage
   */
  async saveCertificates() {
    const certsArray = Array.from(this.certificates.values()).map((cert) =>
      cert.toJSON()
    );
    await writeJSONAsync(CERTIFICATES_FILE, certsArray);
  }

  /**
   * Register a new certificate
   * @param {string} certificateId - Unique certificate ID
   * @param {Object} payload - Certificate payload
   * @param {string} signature - RSA signature
   * @param {string} issuerPublicKey - Issuer's public key
   * @returns {Promise<Certificate>} Registered certificate
   */
  async registerCertificate(
    certificateId,
    payload,
    signature,
    issuerPublicKey
  ) {
    // Check if certificate already exists
    if (this.certificates.has(certificateId)) {
      throw new Error("Certificate already exists");
    }

    // Validate signature
    const payloadString = JSON.stringify(payload);
    const isValid = verifySignature(issuerPublicKey, payloadString, signature);

    if (!isValid) {
      throw new Error("Invalid certificate signature");
    }

    // Create and store certificate
    const certificate = new Certificate(
      certificateId,
      payload,
      signature,
      issuerPublicKey
    );
    this.certificates.set(certificateId, certificate);
    await this.saveCertificates();

    return certificate;
  }

  /**
   * Get certificate by ID
   * @param {string} certificateId - Certificate ID
   * @returns {Certificate|null} Certificate or null if not found
   */
  getCertificate(certificateId) {
    return this.certificates.get(certificateId) || null;
  }

  /**
   * Get all certificates
   * @returns {Certificate[]} All certificates
   */
  getAllCertificates() {
    return Array.from(this.certificates.values());
  }

  /**
   * Verify certificate validity
   * @param {string} certificateId - Certificate ID
   * @param {string} token - Plaintext token to verify
   * @returns {Object} Verification result
   */
  verifyCertificate(certificateId, token) {
    const certificate = this.getCertificate(certificateId);

    if (!certificate) {
      return { valid: false, reason: "Certificate not found" };
    }

    // Check expiry
    if (certificate.isExpired()) {
      return { valid: false, reason: "Certificate expired" };
    }

    // Verify token hash
    const tokenHash = sha256(token);
    if (tokenHash !== certificate.payload.tokenHash) {
      return { valid: false, reason: "Token hash mismatch" };
    }

    // Verify signature
    const payloadString = JSON.stringify(certificate.payload);
    const isValid = verifySignature(
      certificate.issuerPublicKey,
      payloadString,
      certificate.signature
    );

    if (!isValid) {
      return { valid: false, reason: "Invalid signature" };
    }

    return { valid: true, certificate };
  }

  /**
   * Get certificates by issuer
   * @param {string} issuerUserId - Issuer user ID
   * @returns {Certificate[]} Certificates issued by user
   */
  getCertificatesByIssuer(issuerUserId) {
    return this.getAllCertificates().filter(
      (cert) => cert.payload.issuerUserId === issuerUserId
    );
  }

  /**
   * Get certificate statistics
   * @returns {Object} Statistics
   */
  getStats() {
    const all = this.getAllCertificates();
    const expired = all.filter((cert) => cert.isExpired()).length;

    return {
      total: all.length,
      active: all.length - expired,
      expired: expired,
    };
  }
}

// Singleton instance
export const certificateService = new CertificateService();
