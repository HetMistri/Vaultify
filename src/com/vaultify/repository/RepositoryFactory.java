package com.vaultify.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central factory for creating repository instances based on configuration.
 * Reads storage.mode from config.properties (classpath root).
 * Values:
 * - dual (default): Dual*Repository implementations
 * - jdbc: Postgres*Repository implementations only
 * - file: File*Repository implementations only
 */
public final class RepositoryFactory {
    private static final String CONFIG_FILE = "config.properties";
    private static final String MODE_KEY = "storage.mode";
    private static final RepositoryFactory INSTANCE = new RepositoryFactory();

    private final String storageMode;

    private RepositoryFactory() {
        this.storageMode = loadMode();
        System.out.println("[RepositoryFactory] storage.mode=" + storageMode);
    }

    public static RepositoryFactory get() {
        return INSTANCE;
    }

    private String loadMode() {
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null)
                props.load(in);
        } catch (IOException ignored) {
        }
        String mode = props.getProperty(MODE_KEY, "dual").trim().toLowerCase();
        switch (mode) {
            case "jdbc":
            case "file":
            case "dual":
                return mode;
            default:
                return "dual"; // safe default
        }
    }

    // User Repository
    public UserRepository userRepository() {
        switch (storageMode) {
            case "jdbc":
                return new PostgresUserRepository();
            case "file":
                return new FileUserRepository();
            case "dual":
            default:
                return new DualUserRepository(new PostgresUserRepository(), new FileUserRepository());
        }
    }

    // Credential Repository
    public CredentialRepository credentialRepository() {
        switch (storageMode) {
            case "jdbc":
                return new PostgresCredentialRepository();
            case "file":
                return new FileCredentialRepository();
            case "dual":
            default:
                return new DualCredentialRepository(new PostgresCredentialRepository(), new FileCredentialRepository());
        }
    }

    // Token Repository
    public TokenRepository tokenRepository() {
        switch (storageMode) {
            case "jdbc":
                return new PostgresTokenRepository();
            case "file":
                return new FileTokenRepository();
            case "dual":
            default:
                return new DualTokenRepository(new PostgresTokenRepository(), new FileTokenRepository());
        }
    }
}
