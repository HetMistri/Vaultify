package com.vaultify.crypto;

import java.security.*;
import javax.crypto.Cipher;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class RSAEngine implements CryptoEngine {

    @Override
    public byte[] encrypt(byte[] data) throws Exception {
        throw new UnsupportedOperationException("Use encryptWithKey(publicKey)");
    }

    @Override
    public byte[] decrypt(byte[] data) throws Exception {
        throw new UnsupportedOperationException("Use decryptWithKey(privateKey)");
    }

    public static KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keySize);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptWithKey(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                new MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        return cipher.doFinal(data);
    }

    public static byte[] decryptWithKey(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                new MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
        return cipher.doFinal(data);
    }
}
