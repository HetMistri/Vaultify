package com.vaultify.service;

import com.vaultify.crypto.HashUtil;
import com.vaultify.threading.ThreadManager;

import java.util.UUID;

/**
 * Lightweight VaultService implementation for Day -1.
 * Methods are intentionally minimal: they demonstrate a ledger append
 * and asynchronous behavior so the rest of the system can be exercised.
 */
public class VaultService {
    private final LedgerService ledgerService;

    public VaultService() {
        this.ledgerService = new LedgerService();
    }

    /**
     * Add a credential (Day -1 stub).
     * Produces a random credential id and appends a ledger block asynchronously.
     */
    public void addCredential() {
        String credentialId = UUID.randomUUID().toString();
        String dataHash = HashUtil.sha256(credentialId + ":" + System.currentTimeMillis());

        ThreadManager.runAsync(() -> ledgerService.appendBlock("ADD_CREDENTIAL", dataHash));

        System.out.println("VaultService.addCredential(): queued add for id=" + credentialId);
    }

    /**
     * List credentials (Day -1 stub): no persistence yet, so print a helpful
     * message.
     */
    public void listCredentials() {
        System.out.println("VaultService.listCredentials(): not implemented - no persistence layer yet.");
    }

    /**
     * Delete credential (Day -1 stub): create a fake id and append a ledger block.
     */
    public void deleteCredential() {
        String credentialId = UUID.randomUUID().toString();
        String dataHash = HashUtil.sha256("DELETE:" + credentialId + ":" + System.currentTimeMillis());

        ThreadManager.runAsync(() -> ledgerService.appendBlock("DELETE_CREDENTIAL", dataHash));

        System.out.println("VaultService.deleteCredential(): queued delete for id=" + credentialId);
    }
}
