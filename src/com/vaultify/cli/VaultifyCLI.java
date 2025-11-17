package com.vaultify.cli;

/**
 * Entry point for Vaultify CLI.
 * Run with: java com.vaultify.cli.VaultifyCLI
 */
public class VaultifyCLI {
    public static void main(String[] args) {
        System.out.println("=== Vaultify CLI ===");
        new CommandParser().start();
    }
}
