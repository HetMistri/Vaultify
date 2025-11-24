import { ledgerService } from "../services/LedgerService.js";
import { sha256 } from "../utils/crypto.js";

/**
 * Ledger Controller
 * Handles HTTP requests for ledger operations
 */

/**
 * Append a new block to the ledger
 * POST /api/ledger/blocks
 */
export const appendBlock = async (req, res) => {
  try {
    const { action, dataHash } = req.body;

    // Validate input
    if (!action || !dataHash) {
      return res.status(400).json({
        error: "Missing required fields: action, dataHash",
      });
    }

    // Append block
    const block = await ledgerService.appendBlock(action, dataHash);

    res.status(201).json({
      message: "Block appended successfully",
      block: block.toJSON(),
    });
  } catch (error) {
    console.error("Error appending block:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get block by hash
 * GET /api/ledger/blocks/:hash
 */
export const getBlockByHash = (req, res) => {
  try {
    const { hash } = req.params;
    const block = ledgerService.getBlockByHash(hash);

    if (!block) {
      return res.status(404).json({ error: "Block not found" });
    }

    res.json(block.toJSON());
  } catch (error) {
    console.error("Error getting block:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get block by index
 * GET /api/ledger/blocks/index/:index
 */
export const getBlockByIndex = (req, res) => {
  try {
    const index = parseInt(req.params.index);

    if (isNaN(index)) {
      return res.status(400).json({ error: "Invalid index" });
    }

    const block = ledgerService.getBlockByIndex(index);

    if (!block) {
      return res.status(404).json({ error: "Block not found" });
    }

    res.json(block.toJSON());
  } catch (error) {
    console.error("Error getting block:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get all blocks
 * GET /api/ledger/blocks
 */
export const getAllBlocks = (req, res) => {
  try {
    const blocks = ledgerService.getAllBlocks();
    res.json({
      total: blocks.length,
      blocks: blocks.map((b) => b.toJSON()),
    });
  } catch (error) {
    console.error("Error getting blocks:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Verify chain integrity
 * GET /api/ledger/verify
 */
export const verifyChain = (req, res) => {
  try {
    const isValid = ledgerService.verifyChain();
    const stats = ledgerService.getStats();

    res.json({
      valid: isValid,
      stats,
    });
  } catch (error) {
    console.error("Error verifying chain:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Get ledger statistics
 * GET /api/ledger/stats
 */
export const getStats = (req, res) => {
  try {
    const stats = ledgerService.getStats();
    res.json(stats);
  } catch (error) {
    console.error("Error getting stats:", error);
    res.status(500).json({ error: error.message });
  }
};
