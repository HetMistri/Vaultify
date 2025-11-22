package com.vaultify.app;

import java.util.concurrent.TimeUnit;

import com.vaultify.cli.CommandRouter;
import com.vaultify.threading.ThreadManager;
import com.vaultify.threading.TokenCleanupTask;

public class VaultifyApplication {
    public static void main(String[] args) {
        System.out.println("Vaultify CLI starting...");

        // Schedule token expiry cleanup (runs every hour)
        ThreadManager.scheduleAtFixedRate(
                new TokenCleanupTask(),
                0, // Initial delay
                3600, // Period: 1 hour
                TimeUnit.SECONDS);
        System.out.println("Token cleanup scheduler started (runs hourly)");

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down Vaultify...");
            ThreadManager.shutdown();
            System.out.println("Vaultify shutdown complete");
        }));

        // Start CLI
        new CommandRouter().start();
    }
}
