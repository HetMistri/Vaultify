package com.vaultify.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Day 1 skeleton for RSA utilities.
 * Provides method signatures for key generation and basic encrypt/decrypt.
 * Implementations will be added in a later iteration (RSA/OAEP recommended).
 */
public class RSAUtil {

	/**
	 * Generate an RSA keypair with the given key size (e.g., 2048 or 3072).
	 */
	public static KeyPair generateKeyPair(int keySize) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keySize);
			return kpg.generateKeyPair();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate RSA keypair", e);
		}
	}

	/**
	 * Encrypt the provided bytes with a public key. Day 1 stub.
	 */
	public static byte[] encrypt(byte[] data, PublicKey publicKey) {
		// TODO: implement RSA/OAEP encryption
		return new byte[0];
	}

	/**
	 * Decrypt the provided bytes with a private key. Day 1 stub.
	 */
	public static byte[] decrypt(byte[] data, PrivateKey privateKey) {
		// TODO: implement RSA/OAEP decryption
		return new byte[0];
	}
}
