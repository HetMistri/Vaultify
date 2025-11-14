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
                //Add more case later for different commands
                case "help":
                    System.out.println("Available commands: verify, login, vault, exit");
                    break;

                case "verify":
                    System.out.println("Usage: verify <token> <certificate_path>");
                    break;

                case "login":
                    System.out.println("Login not implemented yet.");
                    break;

                case "vault":
                    System.out.println("Vault commands not implemented yet.");
                    break;

                case "exit":
                    System.out.println("Exiting Vaultify CLI.");
                    scanner.close();
                    return;

                default:
                    System.out.println("Unknown command: " + cmd);
            }
        }
    }
}
