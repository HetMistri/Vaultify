package com.vaultify.cli;

import java.util.Scanner;

public class CommandRouter {

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("vaultify> ");
            System.out.flush();

            String cmd = scanner.nextLine();

            switch (cmd) {
                case "help" -> System.out.println("Available commands: verify, login, vault, exit");

                case "verify" -> System.out.println("Usage: verify <token> <certificate_path>");

                case "login" -> System.out.println("Login not implemented yet.");

                case "vault" -> System.out.println("Vault commands not implemented yet.");

                case "exit" -> {
                    System.out.println("Exiting Vaultify CLI.");
                    scanner.close();
                    return;
                }

                default -> System.out.println("Unknown command: " + cmd);
            }
            //Add more case later for different commands
                    }
    }
}
