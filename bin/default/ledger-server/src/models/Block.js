import { sha256 } from "../utils/crypto.js";

/**
 * Ledger Block Model
 * Represents a single block in the blockchain
 */
export class Block {
  constructor(index, action, dataHash, prevHash = "0") {
    this.index = index;
    this.timestamp = Date.now();
    this.action = action;
    this.dataHash = dataHash;
    this.prevHash = prevHash;
    this.hash = this.calculateHash();
  }

  /**
   * Calculate block hash
   * @returns {string} SHA-256 hash of block contents
   */
  calculateHash() {
    const data = `${this.index}${this.timestamp}${this.action}${this.dataHash}${this.prevHash}`;
    return sha256(data);
  }

  /**
   * Validate block integrity
   * @returns {boolean} True if hash is valid
   */
  isValid() {
    return this.hash === this.calculateHash();
  }

  /**
   * Convert block to JSON
   * @returns {Object} JSON representation
   */
  toJSON() {
    return {
      index: this.index,
      timestamp: this.timestamp,
      action: this.action,
      dataHash: this.dataHash,
      prevHash: this.prevHash,
      hash: this.hash,
    };
  }

  /**
   * Create block from JSON
   * @param {Object} json - JSON object
   * @returns {Block} Block instance
   */
  static fromJSON(json) {
    const block = new Block(
      json.index,
      json.action,
      json.dataHash,
      json.prevHash
    );
    block.timestamp = json.timestamp;
    block.hash = json.hash;
    return block;
  }
}
