package com.vaultify.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import com.vaultify.models.User;
import com.vaultify.service.AuthService;
import com.vaultify.service.VerificationService;
import com.vaultify.verifier.CertificateVerifier;

/**
 * CommandRouter for Vaultify CLI.
 * Handles user commands with real authentication and session management.
 */
public class CommandRouter {

    // Services
    private static final AuthService authService = new AuthService();
    private static final VerificationService verificationService = new VerificationService();

    // -------------------------
    // Instance entrypoint used by VaultifyApplication
    // -------------------------
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("vaultify> ");
                String command = scanner.nextLine().trim();
                if (command.isEmpty())
                    continue;
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
            case "logout" -> logout();
            case "whoami" -> whoami();
            case "vault" -> vault(scanner);
            case "verify" -> verifyToken(scanner);
            case "verify-ledger" -> verifyLedger();
            case "share" -> share(scanner);
            case "verify-cert" -> verifyCertificate(scanner);
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

        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine();
        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match.");
            return;
        }

        try {
            User user = authService.register(username, password);
            if (user == null) {
                System.out.println("Username already exists.");
                return;
            }
            System.out.println("User '" + username + "' registered successfully.");
            System.out.println("RSA key pair generated and private key encrypted.");
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // ---------------------------
    // login
    // ---------------------------
    private static void login(Scanner scanner) {
        if (authService.isLoggedIn()) {
            System.out.println("Already logged in as: " + authService.getCurrentUser().getUsername());
            return;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        boolean success = authService.login(username, password);
        if (success) {
            System.out.println("Login successful. Welcome, " + username + "!");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    // ---------------------------
    // logout
    // ---------------------------
    private static void logout() {
        if (!authService.isLoggedIn()) {
            System.out.println("No user is currently logged in.");
            return;
        }
        String username = authService.getCurrentUser().getUsername();
        authService.logout();
        System.out.println("User '" + username + "' logged out successfully.");
    }

    // ---------------------------
    // whoami
    // ---------------------------
    private static void whoami() {
        if (!authService.isLoggedIn()) {
            System.out.println("No user is currently logged in.");
            return;
        }
        User user = authService.getCurrentUser();
        System.out.println("Logged in as: " + user.getUsername());
        System.out.println("User ID: " + user.getId());
    }

    // ---------------------------
    // Vault submenu
    // ---------------------------
    private static void vault(Scanner scanner) {
        if (!authService.isLoggedIn()) {
            System.out.println("Please login first to access vault.");
            return;
        }
        System.out.println("Entering Vault subsystem. Type 'help' for vault commands, 'back' to return.");
        while (true) {
            System.out.print("vaultify:vault> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

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
        // TODO: Integrate with VaultService when vault commands are fully wired
        System.out.println("Credential vault operations will be integrated with VaultService.");
        System.out.println("VaultService is ready - CLI wiring pending.");
    }

    private static void listCredentials() {
        // TODO: Integrate with VaultService
        System.out.println("Credential listing will be integrated with VaultService.");
    }

    private static void viewCredential(String id) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }
        // TODO: Integrate with VaultService
        System.out.println("Credential retrieval will be integrated with VaultService.");
    }

    private static void deleteCredential(String id) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }
        // TODO: Integrate with VaultService
        System.out.println("Credential deletion will be integrated with VaultService.");
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
    // share
    // ---------------------------
    private static void share(Scanner scanner) {
        try {
            System.out.print("Enter issuer user ID: ");
            String userIdStr = scanner.nextLine().trim();
            long userId = Long.parseLong(userIdStr);

            System.out.print("Enter credential ID: ");
            String credentialIdStr = scanner.nextLine().trim();
            long credentialId = Long.parseLong(credentialIdStr);

            System.out.print("Enter issuer PRIVATE key path: ");
            String privPath = scanner.nextLine().trim();

            System.out.print("Expiry in hours (default 48): ");
            String exp = scanner.nextLine().trim();
            int expiryHours = exp.isEmpty() ? 48 : Integer.parseInt(exp);

            Path privateKeyPath = Paths.get(privPath);

            Path certPath = verificationService.generateShareToken(
                    userId,
                    credentialId,
                    privateKeyPath,
                    expiryHours);

            System.out.println("Certificate generated at:");
            System.out.println(certPath.toAbsolutePath());

        } catch (NumberFormatException nfe) {
            System.out.println("Invalid numeric input. Please enter numeric user ID and credential ID.");
        } catch (Exception e) {
            System.out.println("Error generating share token: " + e.getMessage());
        }
    }

    // ---------------------------
    // verify certificate
    // ---------------------------
    private static void verifyCertificate(Scanner scanner) {
        try {
            System.out.print("Enter certificate path: ");
            Path certPath = Paths.get(scanner.nextLine().trim());

            System.out.print("Enter issuer PUBLIC key path: ");
            Path pubPath = Paths.get(scanner.nextLine().trim());

            CertificateVerifier.Result res = verificationService.verifyCertificate(certPath, pubPath);

            System.out.println("\n=== Verification Result ===");
            System.out.println("Valid: " + res.valid);
            System.out.println("Message: " + res.message);
            System.out.println("===========================");

        } catch (Exception e) {
            System.out.println("Error verifying certificate: " + e.getMessage());
        }
    }

    // ---------------------------
    // help
    // ---------------------------
    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  register       - create a new user with RSA key pair");
        System.out.println("  login          - login with username/password");
        System.out.println("  logout         - logout current user");
        System.out.println("  whoami         - show current logged-in user");
        System.out.println("  vault          - vault operations (add/list/view/delete)");
        System.out.println("  share          - generate a share token + signed certificate");
        System.out.println("  verify         - verify shared token (placeholder)");
        System.out.println("  verify-ledger  - verify ledger (placeholder)");
        System.out.println("  verify-cert    - verify a certificate file");
        System.out.println("  help           - show this help");
        System.out.println("  exit           - quit CLI");
    }
}
