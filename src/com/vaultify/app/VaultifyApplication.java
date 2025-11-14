package com.vaultify.app;

import com.vaultify.cli.CommandRouter;

public class VaultifyApplication {
    public static void main(String[] args) {
        System.out.println("Vaultify CLI starting...");
        new CommandRouter().start();
    }
}
