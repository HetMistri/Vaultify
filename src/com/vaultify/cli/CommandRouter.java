package com.vaultify.cli;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * CommandRouter for Vaultify CLI.
 *
 * - public void start()   <-- used by VaultifyApplication
 * - public static handle(String, Scanner) <-- can be reused elsewhere
 *
 * This is a Day-1 demo implementation:
 * - in-memory user store (username -> sha256(password))
 * - in-memory credential store (id -> name)
 *
 * Replace with real services/DAO when available.
 */
public class CommandRouter {

    // In-memory user store for quick Day-1 use (username -> sha256(password))
    private static final Map<String, String> users = new HashMap<>();

    // In-memory credential store for Day-1 demo: id -> credentialName
    private static final Map<String, String> credentials = new HashMap<>();
    private static int credentialCounter = 0;

    // -------------------------
    // Instance entrypoint used by VaultifyApplication
    // -------------------------
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("vaultify> ");
                String command = scanner.nextLine().trim();
                if (command.isEmpty()) continue;
                handle(command, scanner);
            }
        }
    }

    // -------------------------
    // Static router (reusable)
    // -------------------------
    public static void handle(String command, Scanner scanner) {
        switch (command) {
            case "register" -> register(scanner);
            case "login" -> login(scanner);
            case "vault" -> vault(scanner);
            case "verify" -> verifyToken(scanner);
            case "verify-ledger" -> verifyLedger();
            case "help" -> printHelp();
            case "exit" -> {
                System.out.println("Exiting Vaultify CLI...");
                System.exit(0);
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }

    // ---------------------------
    // register
    // ---------------------------
    private static void register(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        if (users.containsKey(username)) {
            System.out.println("Username already exists.");
            return;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String hashed = sha256(password);
        users.put(username, hashed);
        System.out.println("User '" + username + "' registered successfully.");
    }

    // ---------------------------
    // login
    // ---------------------------
    private static void login(Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        String stored = users.get(username);
        if (stored == null) {
            System.out.println("Invalid credentials.");
            return;
        }

        String hashed = sha256(password);
        if (stored.equals(hashed)) {
            System.out.println("Login successful.");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    // ---------------------------
    // Vault submenu
    // ---------------------------
    private static void vault(Scanner scanner) {
        System.out.println("Entering Vault subsystem. Type 'help' for vault commands, 'back' to return.");
        while (true) {
            System.out.print("vaultify:vault> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";

            switch (cmd) {
                case "help" -> printVaultHelp();
                case "add" -> {
                    String name = arg;
                    if (name.isEmpty()) {
                        System.out.print("Enter credential name: ");
                        name = scanner.nextLine().trim();
                    }
                    addCredential(name);
                }
                case "list" -> listCredentials();
                case "view" -> {
                    String id = arg;
                    if (id.isEmpty()) {
                        System.out.print("Enter credential id to view: ");
                        id = scanner.nextLine().trim();
                    }
                    viewCredential(id);
                }
                case "delete" -> {
                    String id = arg;
                    if (id.isEmpty()) {
                        System.out.print("Enter credential id to delete: ");
                        id = scanner.nextLine().trim();
                    }
                    deleteCredential(id);
                }
                case "back" -> {
                    System.out.println("Exiting Vault subsystem.");
                    return;
                }
                default -> System.out.println("Unknown vault command: " + cmd + " (type 'help' for vault commands)");
            }
        }
    }

    private static void printVaultHelp() {
        System.out.println("Vault commands:");
        System.out.println("  add [name]     - add a new credential");
        System.out.println("  list           - list stored credentials");
        System.out.println("  view <id>      - view credential details");
        System.out.println("  delete <id>    - delete a credential");
        System.out.println("  back           - return to top-level CLI");
    }

    private static void addCredential(String name) {
        if (name == null || name.isEmpty()) {
            System.out.println("Credential name cannot be empty.");
            return;
        }
        credentialCounter++;
        String id = String.valueOf(credentialCounter);
        credentials.put(id, name);
        System.out.println("Credential added with id: " + id);
    }

    private static void listCredentials() {
        if (credentials.isEmpty()) {
            System.out.println("No credentials stored.");
            return;
        }
        System.out.println("Stored credentials:");
        credentials.forEach((id, name) -> System.out.println("  " + id + "  " + name));
    }

    private static void viewCredential(String id) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }
        String name = credentials.get(id);
        if (name == null) {
            System.out.println("Credential not found for id: " + id);
        } else {
            // In the real project this would decrypt/read the file; here we print the demo name.
            System.out.println("Credential [" + id + "]: " + name);
        }
    }

    private static void deleteCredential(String id) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }
        if (credentials.remove(id) != null) {
            System.out.println("Credential " + id + " deleted.");
        } else {
            System.out.println("Credential not found: " + id);
        }
    }

    // ---------------------------
    // verify token (placeholder)
    // ---------------------------
    private static void verifyToken(Scanner scanner) {
        System.out.print("Enter share token: ");
        String token = scanner.nextLine().trim();

        System.out.print("Enter certificate path: ");
        String certPath = scanner.nextLine().trim();

        System.out.println("Verification subsystem not implemented in this demo.");
        System.out.println("Received token: " + token + " and certPath: " + certPath);
    }

    // ---------------------------
    // verify-ledger (placeholder)
    // ---------------------------
    private static void verifyLedger() {
        System.out.println("Ledger verification not implemented in this demo.");
    }

    // ---------------------------
    // help
    // ---------------------------
    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  register       - create a new user");
        System.out.println("  login          - login with username/password");
        System.out.println("  vault          - vault operations (add/list/view/delete)");
        System.out.println("  verify         - verify shared token (placeholder)");
        System.out.println("  verify-ledger  - verify ledger (placeholder)");
        System.out.println("  help           - show this help");
        System.out.println("  exit           - quit CLI");
    }

    // ---------------------------
    // small SHA-256 helper
    // ---------------------------
    private static String sha256(String input) {
        if (input == null) input = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
