package com.vaultify.service;

public class VerificationService {
    private final LedgerService ledgerService;

    public VerificationService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }
}
