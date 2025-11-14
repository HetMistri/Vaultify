package com.vaultify.crypto;

import java.security.SecureRandom;

/**
 * Day 1 skeleton for AES utilities.
 * Intentionally minimal with TODOs, so higher layers can compile against
 * expected method signatures. Real AES-GCM implementation will be added later.
 */
public class AESUtil {
	public static final int AES_256_KEY_BYTES = 32; // 256-bit
	public static final int GCM_IV_BYTES = 12;      // 96-bit IV for GCM

	private static final SecureRandom RNG = new SecureRandom();

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
	public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) {
		// TODO: implement AES-GCM encryption
		return new byte[0];
	}

	/**
	 * Decrypt ciphertext with AES (intended: AES-GCM). Day 1 stub.
	 */
	public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) {
		// TODO: implement AES-GCM decryption
		return new byte[0];
	}
}
