package com.vaultify.verifier;

public class Certificate {
    public String token;
    public long issuerUserId;
    public long credentialId;
    public long expiryEpochMs;
    public String payloadHash;        // sha256 hex of token|issuer|credential|expiry
    public String signatureBase64;    // signature of payloadHash (base64)
    public String ledgerBlockHash;    // the ledger block hash that references payloadHash
    public long createdAtMs;
}
