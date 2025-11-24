import { Block } from "../models/Block.js";
import { readJSON, writeJSON } from "../utils/storage.js";
import { sha256 } from "../utils/crypto.js";

const LEDGER_FILE = "ledger.json";

/**
 * Ledger Service
 * Manages the immutable blockchain ledger
 */
export class LedgerService {
  constructor() {
    this.chain = this.loadChain();
  }

  /**
   * Load blockchain from storage
   * @returns {Block[]} Array of blocks
   */
  loadChain() {
    const chainData = readJSON(LEDGER_FILE, null);

    if (!chainData || chainData.length === 0) {
      // Create genesis block
      const genesis = this.createGenesisBlock();
      writeJSON(LEDGER_FILE, [genesis.toJSON()]);
      return [genesis];
    }

    return chainData.map((blockData) => Block.fromJSON(blockData));
  }

  /**
   * Save blockchain to storage
   */
  saveChain() {
    const chainData = this.chain.map((block) => block.toJSON());
    writeJSON(LEDGER_FILE, chainData);
  }

  /**
   * Create the genesis block (first block)
   * @returns {Block} Genesis block
   */
  createGenesisBlock() {
    return new Block(
      0,
      "GENESIS",
      sha256("Vaultify Ledger Genesis Block"),
      "0"
    );
  }

  /**
   * Get the latest block
   * @returns {Block} Latest block
   */
  getLatestBlock() {
    return this.chain[this.chain.length - 1];
  }

  /**
   * Append a new block to the chain
   * @param {string} action - Action type (e.g., 'GENERATE_CERT', 'REVOKE_TOKEN')
   * @param {string} dataHash - SHA-256 hash of associated data
   * @returns {Block} Newly created block
   */
  appendBlock(action, dataHash) {
    const prevBlock = this.getLatestBlock();
    const newBlock = new Block(
      prevBlock.index + 1,
      action,
      dataHash,
      prevBlock.hash
    );

    this.chain.push(newBlock);
    this.saveChain();

    return newBlock;
  }

  /**
   * Get block by hash
   * @param {string} hash - Block hash
   * @returns {Block|null} Block or null if not found
   */
  getBlockByHash(hash) {
    return this.chain.find((block) => block.hash === hash) || null;
  }

  /**
   * Get block by index
   * @param {number} index - Block index
   * @returns {Block|null} Block or null if not found
   */
  getBlockByIndex(index) {
    return this.chain[index] || null;
  }

  /**
   * Get all blocks
   * @returns {Block[]} All blocks
   */
  getAllBlocks() {
    return this.chain;
  }

  /**
   * Verify chain integrity
   * @returns {boolean} True if chain is valid
   */
  verifyChain() {
    for (let i = 1; i < this.chain.length; i++) {
      const currentBlock = this.chain[i];
      const prevBlock = this.chain[i - 1];

      // Check block hash
      if (!currentBlock.isValid()) {
        console.error(`Block ${i} has invalid hash`);
        return false;
      }

      // Check link to previous block
      if (currentBlock.prevHash !== prevBlock.hash) {
        console.error(`Block ${i} has invalid prevHash`);
        return false;
      }
    }

    return true;
  }

  /**
   * Get chain statistics
   * @returns {Object} Chain statistics
   */
  getStats() {
    return {
      totalBlocks: this.chain.length,
      genesisTimestamp: this.chain[0].timestamp,
      latestTimestamp: this.getLatestBlock().timestamp,
      isValid: this.verifyChain(),
    };
  }
}

// Singleton instance
export const ledgerService = new LedgerService();
