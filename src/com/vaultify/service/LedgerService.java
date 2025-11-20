package com.vaultify.service;

import java.util.List;

import com.vaultify.ledger.LedgerBlock;
import com.vaultify.ledger.LedgerEngine;

/**
 * Small service facade around the LedgerEngine to make it easier for other
 * services/CLI to interact with the ledger without depending on the engine
 * implementation details.
 */
public class LedgerService {
    private final LedgerEngine engine;

    public LedgerService() {
        this.engine = new LedgerEngine();
    }

    public LedgerBlock appendBlock(String action, String dataHash) {
        return engine.addBlock(action, dataHash);
    }

    public List<String> verifyIntegrity() {
        return engine.verifyIntegrity();
    }

    public List<LedgerBlock> getChain() {
        return engine.getChain();
    }
}
