package com.vaultify.service;

import com.vaultify.crypto.HashUtil;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight VerificationService for Day -1.
 * Provides minimal token generation and a simple ledger verification helper.
 */
public class VerificationService {
    private final LedgerService ledgerService;

    public VerificationService() {
        this.ledgerService = new LedgerService();
    }

    public VerificationService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    public void generateShareToken() {
        String token = UUID.randomUUID().toString();
        String dataHash = HashUtil.sha256(token + ":" + System.currentTimeMillis());

        // persist to ledger (synchronous here for simplicity)
        ledgerService.appendBlock("GENERATE_TOKEN", dataHash);

        System.out.println("VerificationService.generateShareToken(): token=" + token);
    }

    public void verifyShareToken() {
        List<String> errors = ledgerService.verifyIntegrity();
        if (errors.isEmpty()) {
            System.out.println("VerificationService.verifyShareToken(): ledger OK (no errors)");
        } else {
            System.out.println("VerificationService.verifyShareToken(): ledger errors found:");
            errors.forEach(e -> System.out.println(" - " + e));
        }
    }
}
