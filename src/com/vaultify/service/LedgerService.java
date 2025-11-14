package com.vaultify.service;

import com.vaultify.ledger.LedgerEngine;

public class LedgerService {
    private final LedgerEngine ledgerEngine;
    public LedgerService(LedgerEngine engine) {
        this.ledgerEngine = engine;
    }
}
