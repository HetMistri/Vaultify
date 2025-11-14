package com.vaultify.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Day 1 skeleton for key management. Future responsibilities:
 * - Persist user RSA keypairs to disk (PEM/PKCS8), load them when needed
 * - Protect private keys appropriately
 */
public class KeyManager {

    /**
     * Load an existing keypair for the given user or generate a new one.
     * Day 1 stub.
     */
    public KeyPair getOrCreateUserKeyPair(String userId) {
        // TODO: attempt to load keys; if absent, generate and persist
        return null;
    }

    /**
     * Save a public/private keypair to the provided paths. Day 1 stub.
     */
    public void saveKeyPair(KeyPair keyPair, String publicKeyPath, String privateKeyPath) {
        // TODO: persist keys in PEM/PKCS8 format
    }

    /**
     * Load a public key from a path. Day 1 stub.
     */
    public PublicKey loadPublicKey(String path) {
        // TODO: read and parse PEM-encoded public key
        return null;
    }

    /**
     * Load a private key from a path. Day 1 stub.
     */
    public PrivateKey loadPrivateKey(String path) {
        // TODO: read and parse PKCS#8 PEM-encoded private key
        return null;
    }
}
