package com.vaultify.crypto;

import java.security.SecureRandom;

/**
 * Day 1 AES implementation conforming to CryptoEngine interface.
 * Provides AES-GCM encryption/decryption plus key/IV generation utilities.
 * Full AES-GCM implementation will be added in later iterations.
 */
public class AESEngine implements CryptoEngine {
    public static final int AES_256_KEY_BYTES = 32; // 256-bit
    public static final int GCM_IV_BYTES = 12; // 96-bit IV for GCM

    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public byte[] encrypt(byte[] data) {
        // TODO: implement AES-GCM encryption
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] data) {
        // TODO: implement AES-GCM decryption
        return new byte[0];
    }

    // Additional utility methods for AES-specific operations

    /**
     * Generate a random 256-bit AES key.
     */
    public static byte[] generateKey() {
        byte[] key = new byte[AES_256_KEY_BYTES];
        RNG.nextBytes(key);
        return key;
    }

    /**
     * Generate a random 96-bit IV suitable for AES-GCM.
     */
    public static byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        RNG.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt plaintext with AES (intended: AES-GCM). Day 1 stub.
     */
    public static byte[] encryptWithParams(byte[] plaintext, byte[] key, byte[] iv) {
        // TODO: implement AES-GCM encryption
        return new byte[0];
    }

    /**
     * Decrypt ciphertext with AES (intended: AES-GCM). Day 1 stub.
     */
    public static byte[] decryptWithParams(byte[] ciphertext, byte[] key, byte[] iv) {
        // TODO: implement AES-GCM decryption
        return new byte[0];
    }
}
