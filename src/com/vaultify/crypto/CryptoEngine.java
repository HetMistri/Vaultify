package com.vaultify.crypto;

public interface CryptoEngine {

    byte[] encrypt(byte[] data) throws Exception;
    byte[] decrypt(byte[] data) throws Exception;
}
