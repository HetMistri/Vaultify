package com.vaultify.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.vaultify.models.CredentialMetadata;
import com.vaultify.models.Token;
import com.vaultify.models.User;
import com.vaultify.service.AuthService;
import com.vaultify.service.LedgerService;
import com.vaultify.service.TokenService;
import com.vaultify.service.VaultService;
import com.vaultify.service.VerificationService;
import com.vaultify.util.Config;
import com.vaultify.util.PathValidator;
import com.vaultify.verifier.CertificateVerifier;

/**
 * CommandRouter for Vaultify CLI.
 * Handles user commands with real authentication and session management.
 *
 * Extended: stats, health, reconcile/drift-report (non-destructive)
 *
 * Note: reconciliation is read-only and conservative by design.
 */
public class CommandRouter {

    // Services
    private static final AuthService authService = new AuthService();
    private static final VerificationService verificationService = new VerificationService();
    private static final VaultService vaultService = new VaultService();
    private static final TokenService tokenService = new TokenService();
    private static final LedgerService ledgerService = new LedgerService();

    // Storage paths (safe defaults matching existing code)
    private static final Path KEYS_DIR = Paths.get("vault_data/keys");
    private static final Path CERTS_DIR = Paths.get("vault_data/certificates");
    private static final Path STORAGE_DIR = Paths.get("vault_data/credentials");

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
        boolean devMode = Config.isDevMode();

        switch (command) {
            case "register" -> register(scanner);
            case "login" -> login(scanner);
            case "logout" -> logout();
            case "whoami" -> whoami();
            case "vault" -> vault(scanner);
            case "revoke-token" -> revokeToken(scanner);
            case "list-tokens" -> listTokens();
            case "verify-ledger" -> verifyLedger();
            case "help" -> printHelp();
            case "exit" -> {
                System.out.println("Exiting Vaultify CLI...");
                System.exit(0);
            }

            // New commands (non-invasive)
            case "stats" -> showStats();
            case "health" -> showHealth();
            case "reconcile", "drift-report" -> reconcileAndReport(scanner);

            // Dev-only commands
            case "test-ledger" -> {
                if (devMode) {
                    testLedgerConnection();
                } else {
                    System.out.println("✗ Command 'test-ledger' is only available in development mode.");
                }
            }
            case "test-db" -> {
                if (devMode) {
                    testDatabaseConnection();
                } else {
                    System.out.println("✗ Command 'test-db' is only available in development mode.");
                }
            }
            case "reset-all", "reset" -> {
                if (devMode) {
                    resetAll(scanner);
                } else {
                    System.out.println("✗ Command 'reset-all' is only available in development mode.");
                }
            }
            case "dev-mode" -> showDevModeStatus();

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
        System.out.println("User Public Key:\n" + user.getPublicKey());
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

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "help" -> printVaultHelp();
                case "add" -> {
                    System.out.println("Add credential:");
                    System.out.println("  1. File");
                    System.out.println("  2. Text/Password");
                    System.out.print("Choice [1-2]: ");
                    String choice = scanner.nextLine().trim();
                    switch (choice) {
                        case "1" -> {
                            System.out.print("Enter file path: ");
                            String filePath = scanner.nextLine().trim();
                            addCredentialFromFile(filePath, scanner);
                        }
                        case "2" -> addCredentialFromText(scanner);
                        default -> System.out.println("Invalid choice.");
                    }
                }
                case "list" -> listCredentials();
                case "view" -> {
                    String id = parts.length > 1 ? parts[1] : null;
                    if (id == null || id.isEmpty()) {
                        System.out.print("Enter credential id to view: ");
                        id = scanner.nextLine().trim();
                    }
                    viewCredential(id, scanner);
                }
                case "share" -> shareCredential(scanner);
                case "delete" -> {
                    String id = parts.length > 1 ? parts[1] : null;
                    if (id == null || id.isEmpty()) {
                        System.out.print("Enter credential id to delete: ");
                        id = scanner.nextLine().trim();
                    }
                    deleteCredential(id);
                }

                case "verify-cert" -> verifyCertificate(scanner);

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
        System.out.println("  add                   - add credential (interactive)");
        System.out.println("  delete <id>           - delete a credential");
        System.out.println("  list                  - list stored credentials");
        System.out.println("  view <id>             - view credential details");
        System.out.println("  share                 - generate share token + signed certificate for credential");
        System.out.println("  verify-cert           - verify a certificate file with public key");
        System.out.println("  back                  - return to top-level CLI");
    }

    private static void addCredentialFromFile(String filePath, Scanner scanner) {
        try {
            // Validate file path
            PathValidator.ValidationResult validation = PathValidator.validateFilePath(filePath);
            if (!validation.valid) {
                System.out.println("✗ Validation failed: " + validation.message);
                return;
            }

            // Get user's public key
            User user = authService.getCurrentUser();
            PublicKey publicKey = authService.getUserPublicKey(user.getUsername());
            if (publicKey == null) {
                System.out.println("✗ Failed to load user's public key.");
                return;
            }

            // Show file info
            long fileSize = Files.size(validation.normalizedPath);
            System.out.println("\nFile: " + validation.normalizedPath.getFileName());
            System.out.println("Size: " + PathValidator.formatSize(fileSize));
            System.out.print("\nEncrypt and store this file? [y/N]: ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                System.out.println("Cancelled.");
                return;
            }

            // Add credential
            String credentialId = vaultService.addCredential(user.getId(), validation.normalizedPath, publicKey);
            System.out.println("\n✓ Credential added successfully!");
            System.out.println("  ID: " + credentialId);

        } catch (Exception e) {
            System.out.println("✗ Failed to add credential: " + e.getMessage());
            System.err.println("Error: Could not encrypt or store the file. Please check file permissions.");
        }
    }

    private static void addCredentialFromText(Scanner scanner) {
        try {
            System.out.println("\nEnter credential text (press Ctrl+D or Ctrl+Z when done):");
            System.out.println("Examples:");
            System.out.println("  - Password format: username: xyz, app/site: example.com, password: xyz123");
            System.out.println("  - Simple text: Any text you want to encrypt\n");

            StringBuilder text = new StringBuilder();
            System.out.print("> ");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Check for end marker (empty line after content)
                if (line.equals("--end") || line.equals(".")) {
                    break;
                }
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(line);

                // Single line mode - if line is not empty and we have content, ask if done
                if (!text.isEmpty()) {
                    System.out.print("Continue? [y/N] or type '--end' to finish: ");
                    String cont = scanner.nextLine().trim().toLowerCase();
                    if (!cont.equals("y") && !cont.equals("yes")) {
                        if (cont.equals("--end") || cont.equals(".")) {
                            break;
                        }
                        // User wants to finish with current content
                        break;
                    }
                    System.out.print("> ");
                }
            }

            if (text.isEmpty()) {
                System.out.println("No text entered.");
                return;
            }

            // Create temporary file
            Path tempFile = Files.createTempFile("vaultify-text-", ".txt");
            Files.writeString(tempFile, text.toString());

            // Get user's public key
            User user = authService.getCurrentUser();
            PublicKey publicKey = authService.getUserPublicKey(user.getUsername());
            if (publicKey == null) {
                System.out.println("✗ Failed to load user's public key.");
                Files.deleteIfExists(tempFile);
                return;
            }

            // Add credential
            String credentialId = vaultService.addCredential(user.getId(), tempFile, publicKey);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            System.out.println("\n✓ Text credential added successfully!");
            System.out.println("  ID: " + credentialId);

        } catch (Exception e) {
            System.out.println("✗ Failed to add text credential: " + e.getMessage());
            System.err.println("Error: Could not encrypt or store the text. Please try again.");
        }
    }

    private static void listCredentials() {
        try {
            User user = authService.getCurrentUser();
            List<CredentialMetadata> credentials = vaultService.listCredentials(user.getId());

            if (credentials.isEmpty()) {
                System.out.println("No credentials stored.");
                return;
            }

            System.out.println("\n=== Your Credentials ===");
            for (CredentialMetadata meta : credentials) {
                System.out.println("\nID: " + meta.credentialIdString);
                System.out.println("  File: " + meta.filename);
                System.out.println("  Size: " + PathValidator.formatSize(meta.fileSize));
                System.out.println("  Added: " + new java.util.Date(meta.timestamp));
            }
            System.out.println("\nTotal: " + credentials.size() + " credential(s)");

        } catch (Exception e) {
            System.out.println("✗ Failed to list credentials: " + e.getMessage());
        }
    }

    private static void viewCredential(String id, Scanner scanner) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }

        try {
            User user = authService.getCurrentUser();

            // Get user's private key
            java.security.PrivateKey privateKey = authService.getUserPrivateKey(user.getUsername());
            if (privateKey == null) {
                System.out.println("✗ Failed to load user's private key.");
                return;
            }

            // Retrieve and decrypt
            byte[] plaintext = vaultService.retrieveCredential(id, privateKey);

            // Display as text (assuming text content)
            System.out.println("\n=== Credential Content ===");
            System.out.println(new String(plaintext, StandardCharsets.UTF_8));
            System.out.println("\n==========================");

            // Ask if user wants to save to file
            System.out.print("\nSave to file? [y/N]: ");
            String save = scanner.nextLine().trim().toLowerCase();
            if (save.equals("y") || save.equals("yes")) {
                System.out.print("Output file path: ");
                String outPath = scanner.nextLine().trim();
                Files.write(Paths.get(outPath), plaintext);
                System.out.println("✓ Saved to: " + outPath);
            }

        } catch (Exception e) {
            System.out.println("✗ Failed to retrieve credential: " + e.getMessage());
        }
    }

    private static void deleteCredential(String id) {
        if (id == null || id.isEmpty()) {
            System.out.println("Invalid credential id.");
            return;
        }

        try {
            User user = authService.getCurrentUser();
            vaultService.deleteCredential(id, user.getId());
            System.out.println("✓ Credential deleted: " + id);

        } catch (Exception e) {
            System.out.println("✗ Failed to delete credential: " + e.getMessage());
        }
    }

    // ---------------------------
    // share - Generate token and certificate for credential sharing
    // ---------------------------
    private static void shareCredential(Scanner scanner) {
        if (!authService.isLoggedIn()) {
            System.out.println("Please login first.");
            return;
        }

        try {
            User user = authService.getCurrentUser();

            // Get credential ID to share
            System.out.print("Enter credential ID to share: ");
            String credentialId = scanner.nextLine().trim();

            // Validate credential exists and belongs to user
            List<CredentialMetadata> userCreds = vaultService.listCredentials(user.getId());
            CredentialMetadata cred = userCreds.stream()
                    .filter(c -> c.credentialIdString.equals(credentialId))
                    .findFirst()
                    .orElse(null);

            if (cred == null) {
                System.out.println("✗ Credential not found or you don't own it.");
                return;
            }

            System.out.print("Expiry in hours (default 48): ");
            String exp = scanner.nextLine().trim();
            int expiryHours = exp.isEmpty() ? 48 : Integer.parseInt(exp);

            // Generate token (not yet persisted) then persist
            // Use credential ID or hash of UUID string as numeric reference
            long credIdNum = (cred.id != 0) ? cred.id : Math.abs(cred.credentialIdString.hashCode());
            Token token = tokenService.generateToken(user.getId(), credIdNum, expiryHours);
            tokenService.persistToken(token);

            // Get user's private key for signing certificate
            java.security.PrivateKey privateKey = authService.getUserPrivateKey(user.getUsername());
            if (privateKey == null) {
                System.out.println("✗ Failed to load your private key.");
                return;
            }

            // Get user's public key path for certificate (auto-create if missing)
            Path publicKeyPath = Paths.get("vault_data", "keys", user.getUsername() + "_public.pem");
            if (!Files.exists(publicKeyPath)) {
                try {
                    Files.createDirectories(publicKeyPath.getParent());
                    // Reconstruct PEM from Base64 stored in user (X.509 encoded bytes)
                    byte[] pubDer = java.util.Base64.getDecoder().decode(user.getPublicKey());
                    String pemBody = java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pubDer);
                    String pem = "-----BEGIN PUBLIC KEY-----\n" + pemBody + "\n-----END PUBLIC KEY-----\n";
                    Files.writeString(publicKeyPath, pem);
                    System.out.println("[Auto-Recovery] Public key file recreated: " + publicKeyPath);
                } catch (Exception pkEx) {
                    System.out.println("✗ Public key not found and could not be recreated: " + pkEx.getMessage());
                    return;
                }
            }

            // Generate certificate
            Path certOutputDir = Paths.get("vault_data/certificates");
            Files.createDirectories(certOutputDir);
            String tokenHash = com.vaultify.crypto.HashUtil.sha256(token.getToken());
            Path certPath = certOutputDir.resolve("cert-" + tokenHash.substring(0, 16) + ".json");

            tokenService.createCertificate(token, cred, privateKey, publicKeyPath, certPath);

            System.out.println("\n✅ Share token and certificate generated!");
            System.out.println("===========================================");
            System.out.println("📤 SHARE THESE WITH RECIPIENT:");
            System.out.println("   1. Token: " + token.getToken());
            System.out.println("   2. Certificate: " + certPath.toAbsolutePath());
            System.out.println("===========================================");
            System.out.println("📋 Details:");
            System.out.println("   Credential ID: " + cred.credentialIdString);
            System.out.println("   Token Hash: " + tokenHash);
            System.out.println("   Expires: " + new java.util.Date(token.getExpiry().getTime()));
            System.out.println("===========================================");
            System.out.println("\n⚠️  SECURITY NOTES:");
            System.out.println("   • Token is sent ONCE to recipient (confidential)");
            System.out.println("   • Certificate is publicly verifiable");
            System.out.println("   • Only tokenHash stored on ledger server");
            System.out.println("===========================================\n");

        } catch (NumberFormatException nfe) {
            System.out.println("✗ Invalid numeric input.");
        } catch (Exception e) {
            System.out.println("✗ Error generating share token: " + e.getMessage());
        }
    }

    // ---------------------------
    // revoke-token
    // ---------------------------
    private static void revokeToken(Scanner scanner) {
        if (!authService.isLoggedIn()) {
            System.out.println("Please login first.");
            return;
        }

        try {
            System.out.print("Enter token to revoke: ");
            String tokenString = scanner.nextLine().trim();

            // Validate token exists and belongs to user
            Token token = tokenService.validateToken(tokenString);
            if (token == null) {
                System.out.println("✗ Token not found or already expired.");
                return;
            }

            User user = authService.getCurrentUser();
            if (token.getIssuerUserId() != user.getId()) {
                System.out.println("✗ You can only revoke tokens you issued.");
                return;
            }

            tokenService.revokeToken(tokenString);

        } catch (Exception e) {
            System.out.println("✗ Error revoking token: " + e.getMessage());
        }
    }

    // ---------------------------
    // list-tokens
    // ---------------------------
    private static void listTokens() {
        if (!authService.isLoggedIn()) {
            System.out.println("Please login first.");
            return;
        }

        try {
            User user = authService.getCurrentUser();
            List<Token> tokens = tokenService.listUserTokens(user.getId());

            if (tokens.isEmpty()) {
                System.out.println("No tokens generated.");
                return;
            }

            System.out.println("\n=== Your Generated Tokens ===");
            for (Token t : tokens) {
                System.out.println("\nToken: " + t.getToken());
                System.out.println("  Credential ID: " + t.getCredentialId());
                System.out.println("  Expires: " + t.getExpiry());
                System.out.println(
                        "  Status: " + (t.isValid() ? "✓ Valid" : (t.isRevoked() ? "✗ Revoked" : "✗ Expired")));
            }
            System.out.println("\nTotal: " + tokens.size() + " token(s)");

        } catch (Exception e) {
            System.out.println("✗ Error listing tokens: " + e.getMessage());
        }
    }

    // ---------------------------
    // verify-ledger
    // ---------------------------
    private static void verifyLedger() {
        try {
            List<String> errors = ledgerService.verifyIntegrity();

            if (errors.isEmpty()) {
                System.out.println("✓ Ledger integrity verified - no issues found");
                System.out.println("  Total blocks: " + ledgerService.getChain().size());
            } else {
                System.out.println("✗ Ledger integrity check FAILED:");
                errors.forEach(err -> System.out.println("  - " + err));
            }
        } catch (Exception e) {
            System.out.println("✗ Verification error: " + e.getMessage());
        }
    }

    // ---------------------------
    // verify certificate
    // ---------------------------
    private static void verifyCertificate(Scanner scanner) {
        try {
            System.out.print("Enter certificate path: ");
            Path certPath = Paths.get(scanner.nextLine().trim());

            System.out.print("Enter token (the one User A gave you): ");
            String token = scanner.nextLine().trim();

            CertificateVerifier.Result res = verificationService.verifyCertificate(certPath, token);

            if (!res.valid) {
                System.out.println("\n❌ Verification failed: " + res.message);
            }
            // Success message already printed by verifier

        } catch (Exception e) {
            System.out.println("\n✗ Error verifying certificate: " + e.getMessage());
        }
    }

    // ---------------------------
    // test database connection
    // ---------------------------
    private static void testDatabaseConnection() {
        System.out.println("\n=== Database Connection Test ===");

        try (java.sql.Connection conn = com.vaultify.db.Database.getConnection()) {
            System.out.println("✓ Connection successful!");
            System.out.println("  URL: " + conn.getMetaData().getURL());
            System.out.println("  Database: " + conn.getCatalog());
            System.out.println("  User: " + conn.getMetaData().getUserName());

            // List all tables in the database
            System.out.println("\n=== Available Tables ===");
            try (java.sql.ResultSet rs = conn.getMetaData().getTables(null, "public", "%", new String[] { "TABLE" })) {
                boolean foundTables = false;
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    System.out.println("  • " + tableName);
                    foundTables = true;
                }
                if (!foundTables) {
                    System.out.println("  ⚠ No tables found! init.sql did not execute.");
                    System.out.println("\n  Fix: docker compose down -v && docker compose up");
                }
            }

            // Describe each expected table and validate schema
            System.out.println("\n=== Table Schemas ===");
            String[] expectedTables = { "users", "credentials", "tokens" };
            boolean schemaValid = true;

            for (String table : expectedTables) {
                try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, "public", table, null)) {
                    if (!rs.isBeforeFirst()) {
                        System.out.println("\n✗ Table '" + table + "' does NOT exist");
                        schemaValid = false;
                        continue;
                    }

                    System.out.println("\n✓ Table: " + table);
                    int colCount = 0;
                    while (rs.next()) {
                        String colName = rs.getString("COLUMN_NAME");
                        String colType = rs.getString("TYPE_NAME");
                        int colSize = rs.getInt("COLUMN_SIZE");
                        String nullable = rs.getString("IS_NULLABLE").equals("YES") ? "NULL" : "NOT NULL";
                        System.out.println("    - " + colName + " (" + colType +
                                (colSize > 0 ? "(" + colSize + ")" : "") + ") " + nullable);
                        colCount++;
                    }

                    // Validate users table has crypto fields
                    if (table.equals("users") && colCount < 6) {
                        System.out.println(
                                "    ⚠ WARNING: users table has old schema (missing public_key, private_key_encrypted)");
                        schemaValid = false;
                    }
                }
            }

            if (!schemaValid) {
                System.out.println("\n⚠ SCHEMA MISMATCH DETECTED!");
                System.out.println("\nThe database was initialized with an old schema.");
                System.out.println("You MUST recreate the database volume:\n");
                System.out.println("  1. Exit this container (Ctrl+C)");
                System.out.println("  2. Run: docker compose down -v");
                System.out.println("  3. Run: docker compose up\n");
                System.out.println("The -v flag is CRITICAL - it removes the old database volume.");
            } else {
                System.out.println("\n✓ All schemas valid!");
            }

        } catch (java.sql.SQLException e) {
            System.out.println("✗ Connection failed!");
            System.out.println("  Error: " + e.getMessage());
        }
        System.out.println("================================\n");
    }

    // ---------------------------
    // test-ledger
    // ---------------------------
    private static void testLedgerConnection() {
        System.out.println("================================");
        System.out.println("Testing Ledger Server Connection");
        System.out.println("================================");

        System.out.print("Checking server availability... ");
        boolean available = com.vaultify.client.LedgerClient.isServerAvailable();

        if (available) {
            System.out.println("✓ Connected!");
            System.out.println("\nFetching ledger statistics...");

            java.util.List<com.vaultify.models.LedgerBlock> blocks = com.vaultify.client.LedgerClient.getAllBlocks();
            System.out.println("✓ Total blocks: " + blocks.size());

            if (!blocks.isEmpty()) {
                com.vaultify.models.LedgerBlock latest = blocks.get(blocks.size() - 1);
                // defensive: try getIndex/getAction if available
                try {
                    System.out.println("✓ Latest block index: " + latest.getIndex());
                } catch (Throwable ignored) {
                }
                try {
                    System.out.println("✓ Latest block action: " + latest.getAction());
                } catch (Throwable ignored) {
                }
            }

            System.out.print("\nVerifying chain integrity... ");
            boolean valid = com.vaultify.client.LedgerClient.verifyLedgerIntegrity();
            if (valid) {
                System.out.println("✓ Valid");
            } else {
                System.out.println("✗ Invalid");
            }
        } else {
            System.out.println("✗ Not available");
            System.out.println("\n⚠ Make sure the ledger server is running:");
            System.out.println("  cd ledger-server");
            System.out.println("  npm start");
        }

        System.out.println("================================\n");
    }

    // ---------------------------
    // help
    // ---------------------------

    private static void printHelp() {
        boolean devMode = Config.isDevMode();

        System.out.println("\n=== Vaultify CLI Help" + (devMode ? " [DEV MODE]" : " [PRODUCTION]") + " ===");
        System.out.println("\nCore Commands:");
        System.out.println("  register       - create a new user with RSA key pair");
        System.out.println("  login          - login with username/password");
        System.out.println("  logout         - logout current user");
        System.out.println("  whoami         - show current logged-in user");
        System.out.println("  vault          - vault operations (add/list/view/delete credentials)");
        System.out.println("  revoke-token   - revoke a previously generated token");
        System.out.println("  list-tokens    - list all tokens you've generated");
        System.out.println("  verify-ledger  - verify integrity of the blockchain ledger");

        System.out.println("\nMonitoring Commands:");
        System.out.println("  stats          - show system stats (counts, disk usage)");
        System.out.println("  health         - run health checks (DB, ledger, storage)");
        System.out.println("  reconcile      - reconcile DB, stored files and ledger; produce drift report");
        System.out.println("  drift-report   - alias for reconcile");

        if (devMode) {
            System.out.println("\n⚠️  Development Commands (dev.mode=true):");
            System.out.println("  test-ledger    - test connection to remote ledger server");
            System.out.println("  test-db        - test database connection and schema");
            System.out.println("  reset-all      - ⚠️  DELETE ALL DATA (users, credentials, tokens, ledger)");
            System.out.println("  dev-mode       - show current development mode status");
        }

        System.out.println("\nGeneral:");
        System.out.println("  help           - show this help");
        System.out.println("  exit           - quit CLI");
        System.out.println("\n================================================\n");
    }

    // ---------------------------
    // New: stats
    // ---------------------------
    private static void showStats() {
        System.out.println("\n=== Vaultify Stats ===");
        try (java.sql.Connection conn = com.vaultify.db.Database.getConnection()) {

            long users = queryCount(conn, "SELECT COUNT(*) FROM users");
            long credentials = queryCount(conn, "SELECT COUNT(*) FROM credentials");
            long tokens = queryCount(conn, "SELECT COUNT(*) FROM tokens");

            System.out.println("Users       : " + users);
            System.out.println("Credentials : " + credentials);
            System.out.println("Tokens      : " + tokens);

        } catch (Exception e) {
            System.out.println("✗ Could not query database: " + e.getMessage());
        }

        // Disk usage for vault_data
        try {
            long[] usage = directorySizeAndCount(STORAGE_DIR);
            if (usage != null) {
                System.out.println("\nStorage directory: " + STORAGE_DIR.toAbsolutePath());
                System.out.println("  Files: " + usage[1]);
                System.out.println("  Size : " + PathValidator.formatSize(usage[0]));
            } else {
                System.out.println("\nStorage directory not found: " + STORAGE_DIR.toAbsolutePath());
            }
        } catch (Exception ex) {
            System.out.println("✗ Could not inspect storage dir: " + ex.getMessage());
        }

        // Ledger status
        try {
            boolean ledgerAvailable = com.vaultify.client.LedgerClient.isServerAvailable();
            System.out.println("\nLedger server : " + (ledgerAvailable ? "✓ Available" : "✗ Not available"));
            if (ledgerAvailable) {
                List<com.vaultify.models.LedgerBlock> blocks = com.vaultify.client.LedgerClient.getAllBlocks();
                System.out.println("  Blocks       : " + blocks.size());
            }
        } catch (Exception e) {
            System.out.println("✗ Could not contact ledger: " + e.getMessage());
        }

        System.out.println("======================\n");
    }

    // ---------------------------
    // New: health checks
    // ---------------------------
    private static void showHealth() {
        System.out.println("\n=== Vaultify Health Check ===");

        // DB
        System.out.print("Database: ");
        try (java.sql.Connection conn = com.vaultify.db.Database.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ OK (" + conn.getMetaData().getURL() + ")");
            } else {
                System.out.println("✗ Failed to open connection");
            }
        } catch (SQLException e) {
            System.out.println("✗ ERROR - " + e.getMessage());
        }

        // Ledger
        System.out.print("Ledger Server: ");
        try {
            boolean ledgerOk = com.vaultify.client.LedgerClient.isServerAvailable();
            System.out.println(ledgerOk ? "✓ OK" : "✗ Not reachable");
        } catch (Exception e) {
            System.out.println("✗ ERROR - " + e.getMessage());
        }

        // Storage dirs and permissions
        System.out.print("Storage dir (read/write): ");
        Path storage = STORAGE_DIR;
        try {
            if (!Files.exists(storage)) {
                System.out.println("✗ Missing (" + storage.toAbsolutePath() + ")");
            } else {
                boolean r = Files.isReadable(storage);
                boolean w = Files.isWritable(storage);
                System.out.println((r && w) ? "✓ OK" : "✗ Permissions issue (r:" + r + " w:" + w + ")");
            }
        } catch (Exception e) {
            System.out.println("✗ ERROR - " + e.getMessage());
        }

        System.out.print("Keys dir: ");
        try {
            if (!Files.exists(KEYS_DIR)) {
                System.out.println("✗ Missing (" + KEYS_DIR.toAbsolutePath() + ")");
            } else {
                System.out.println("✓ OK (" + KEYS_DIR.toAbsolutePath() + ")");
            }
        } catch (Exception e) {
            System.out.println("✗ ERROR - " + e.getMessage());
        }

        // Quick verification: can we read an example public key if any user exists?
        try {
            long userCount = queryCount(com.vaultify.db.Database.getConnection(), "SELECT COUNT(*) FROM users");
            System.out.print("Public keys present for users: ");
            if (userCount == 0) {
                System.out.println("⚠ No users found");
            } else {
                boolean anyKey = false;
                try (java.sql.Connection c = com.vaultify.db.Database.getConnection();
                        PreparedStatement ps = c
                                .prepareStatement("SELECT public_key FROM users WHERE public_key IS NOT NULL LIMIT 1");
                        ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String pk = rs.getString(1);
                        if (pk != null && !pk.trim().isEmpty()) {
                            anyKey = true;
                        }
                    }
                }
                System.out.println(anyKey ? "✓ Found" : "✗ Missing");
            }
        } catch (SQLException e) {
            System.out.println("✗ ERROR - " + e.getMessage());
        } catch (Exception e) {
            // ignore
        }

        System.out.println("================================\n");
    }

    // ---------------------------
    // New: reconcile & drift reporting (read-only)
    // ---------------------------
    private static void reconcileAndReport(Scanner scanner) {
        System.out.println("\n=== Reconciliation & Drift Report ===");
        System.out.println("This operation will inspect:");
        System.out.println("  - DB 'credentials' table entries");
        System.out.println("  - Stored files under: " + STORAGE_DIR.toAbsolutePath());
        System.out.println("  - Ledger blocks (if ledger server available)\n");

        System.out.print("Proceed? [y/N]: ");
        String proceed = scanner.nextLine().trim().toLowerCase();
        if (!proceed.equals("y") && !proceed.equals("yes")) {
            System.out.println("Cancelled.");
            return;
        }

        try (java.sql.Connection conn = com.vaultify.db.Database.getConnection()) {

            // 1) Load DB credentials
            Map<String, DbCred> dbCreds = new HashMap<>();
            try {
                // Detect whether 'credential_id_string' exists; fall back safely if not present
                boolean hasCredIdString = columnExists(conn, "credentials", "credential_id_string");

                StringBuilder select = new StringBuilder("SELECT id, filename, file_size, user_id");
                if (hasCredIdString)
                    select.append(", credential_id_string");
                select.append(" FROM credentials");

                try (PreparedStatement ps = conn.prepareStatement(select.toString());
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String cid = null;
                        if (hasCredIdString) {
                            try {
                                cid = rs.getString("credential_id_string");
                            } catch (SQLException ignore) {
                                cid = null;
                            }
                        }

                        String filename = null;
                        try {
                            filename = rs.getString("filename");
                        } catch (SQLException ignore) {
                            filename = null;
                        }

                        long size = 0L;
                        try {
                            size = rs.getLong("file_size");
                        } catch (SQLException ignore) {
                            size = 0L;
                        }

                        long userId = 0L;
                        try {
                            userId = rs.getLong("user_id");
                        } catch (SQLException ignore) {
                            userId = 0L;
                        }

                        // If credential id string missing, fall back to filename or id-based
                        // placeholder
                        String effectiveCid = cid;
                        if (effectiveCid == null || effectiveCid.trim().isEmpty()) {
                            if (filename != null && !filename.trim().isEmpty()) {
                                effectiveCid = filename;
                            } else {
                                effectiveCid = String.valueOf(id);
                            }
                        }

                        dbCreds.put(effectiveCid, new DbCred(effectiveCid, id, filename, size, userId));
                    }
                }

            } catch (SQLException ex) {
                System.out.println("✗ Failed to read credentials from DB: " + ex.getMessage());
            }

            System.out.println("DB credentials found: " + dbCreds.size());

            // 2) Inspect storage directory for files (map: credential-id -> path)
            Map<String, Path> storedFiles = new HashMap<>();
            if (Files.exists(STORAGE_DIR) && Files.isDirectory(STORAGE_DIR)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(STORAGE_DIR)) {
                    for (Path p : ds) {
                        if (Files.isRegularFile(p)) {
                            String name = p.getFileName().toString();
                            String key = name;
                            if (name.contains(".")) {
                                key = name.substring(0, name.indexOf('.'));
                            }
                            storedFiles.put(key, p);
                            storedFiles.put(name, p);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("✗ Could not list storage dir: " + e.getMessage());
                }
            } else {
                System.out.println("✗ Storage directory not present: " + STORAGE_DIR.toAbsolutePath());
            }

            System.out.println("Stored files discovered: " + storedFiles.size());

            // 3) Ledger extraction (best-effort, reflection-friendly)
            Set<String> ledgerCredIds = new HashSet<>();
            boolean ledgerAvailable = false;
            try {
                ledgerAvailable = com.vaultify.client.LedgerClient.isServerAvailable();
                if (ledgerAvailable) {
                    List<com.vaultify.models.LedgerBlock> blocks = com.vaultify.client.LedgerClient.getAllBlocks();

                    for (com.vaultify.models.LedgerBlock b : blocks) {
                        String maybe = extractTextFromLedgerBlock(b);
                        if (maybe != null && !maybe.isEmpty()) {
                            String[] tokens = maybe.split("[\\s,\\:\\{\\}\\[\\]\"'\\(\\)<>]+");
                            for (String tok : tokens) {
                                if (tok.length() >= 6 && tok.length() <= 128) {
                                    if (tok.matches(".*[0-9].*") || tok.contains("-")) {
                                        ledgerCredIds.add(tok);
                                    }
                                }
                            }
                        }
                    }
                    System.out.println("Ledger referenced credential-like tokens found: " + ledgerCredIds.size());
                } else {
                    System.out.println("Ledger server not available; skipping ledger checks.");
                }
            } catch (Exception e) {
                System.out.println("✗ Ledger check error: " + e.getMessage());
            }

            // 4) Compare sets and produce report
            List<String> missingFiles = new ArrayList<>();
            List<String> orphanFiles = new ArrayList<>();
            List<String> mismatchedSizes = new ArrayList<>();

            // DB -> File
            for (DbCred d : dbCreds.values()) {
                boolean matched = false;
                if (storedFiles.containsKey(d.credentialId)) {
                    matched = true;
                    Path p = storedFiles.get(d.credentialId);
                    try {
                        long actual = Files.size(p);
                        if (actual != d.fileSize) {
                            mismatchedSizes.add(d.credentialId + " (DB: " + d.fileSize + " vs FS: " + actual + ") -> "
                                    + p.getFileName());
                        }
                    } catch (IOException ioe) {
                        mismatchedSizes.add(d.credentialId + " (could not read file) -> " + p.getFileName());
                    }
                } else {
                    if (d.filename != null && storedFiles.containsKey(d.filename)) {
                        matched = true;
                        Path p = storedFiles.get(d.filename);
                        try {
                            long actual = Files.size(p);
                            if (actual != d.fileSize) {
                                mismatchedSizes.add(d.credentialId + " (DB: " + d.fileSize + " vs FS: " + actual
                                        + ") -> " + p.getFileName());
                            }
                        } catch (IOException ioe) {
                            mismatchedSizes.add(d.credentialId + " (could not read file) -> " + p.getFileName());
                        }
                    }
                }

                if (!matched) {
                    missingFiles.add(d.credentialId + " (expected file: " + d.filename + ")");
                }
            }

            // File -> DB (orphan files)
            Set<String> dbIds = dbCreds.keySet();
            for (Map.Entry<String, Path> ent : storedFiles.entrySet()) {
                String fileKey = ent.getKey();
                if (dbIds.contains(fileKey))
                    continue;
                boolean matchedByFilename = dbCreds.values().stream()
                        .anyMatch(dc -> dc.filename != null && dc.filename.equals(fileKey));
                if (!matchedByFilename) {
                    String realName = ent.getValue().getFileName().toString();
                    boolean matchedReal = dbCreds.values().stream()
                            .anyMatch(dc -> dc.filename != null && dc.filename.equals(realName));
                    if (!matchedReal) {
                        orphanFiles.add(realName + " -> " + ent.getValue().toAbsolutePath());
                    }
                }
            }

            // Ledger -> DB: tokens present in ledger but not in DB
            List<String> ledgerOnly = new ArrayList<>();
            if (!ledgerCredIds.isEmpty()) {
                for (String ledgerId : ledgerCredIds) {
                    if (!dbCreds.containsKey(ledgerId)) {
                        ledgerOnly.add(ledgerId);
                    }
                }
            }

            // 5) Present report
            System.out.println("\n=== Reconciliation Results ===");
            System.out.println("DB credentials: " + dbCreds.size());
            System.out.println("Stored files : " + storedFiles.size());
            if (ledgerAvailable) {
                System.out.println("Ledger tokens: " + ledgerCredIds.size());
            }

            System.out.println("\n-- Missing files for DB entries (" + missingFiles.size() + ")");
            missingFiles.stream().limit(200).forEach(s -> System.out.println("  • " + s));
            if (missingFiles.size() > 200)
                System.out.println("  ... (" + (missingFiles.size() - 200) + " more)");

            System.out.println("\n-- Orphan files on disk (" + orphanFiles.size() + ")");
            orphanFiles.stream().limit(200).forEach(s -> System.out.println("  • " + s));
            if (orphanFiles.size() > 200)
                System.out.println("  ... (" + (orphanFiles.size() - 200) + " more)");

            System.out.println("\n-- Mismatched sizes (" + mismatchedSizes.size() + ")");
            mismatchedSizes.stream().limit(200).forEach(s -> System.out.println("  • " + s));
            if (mismatchedSizes.size() > 200)
                System.out.println("  ... (" + (mismatchedSizes.size() - 200) + " more)");

            if (ledgerAvailable) {
                System.out.println("\n-- Ledger-only tokens/ids (" + ledgerOnly.size() + ")");
                ledgerOnly.stream().limit(200).forEach(s -> System.out.println("  • " + s));
                if (ledgerOnly.size() > 200)
                    System.out.println("  ... (" + (ledgerOnly.size() - 200) + " more)");
            }

            System.out.println("\n=== Suggested next steps ===");
            System.out.println(
                    "  1) Investigate missing files: check backups or device uploads for those credential IDs.");
            System.out.println(
                    "  2) Investigate orphan files: determine if they belong to old users or are transient temp files.");
            System.out.println(
                    "  3) For mismatched sizes: re-download from storage or re-encrypt original source if available.");
            if (ledgerAvailable) {
                System.out.println(
                        "  4) For ledger-only IDs: inspect ledger block payloads (structure differs between deployments).");
            }
            System.out.println("\nReconciliation finished.\n");

        } catch (SQLException e) {
            System.out.println("✗ Reconciliation failed due to DB error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Reconciliation failed: " + e.getMessage());
        }
    }

    // ---------------------------
    // Helper utilities
    // ---------------------------

    private static long queryCount(java.sql.Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private static long[] directorySizeAndCount(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            return null;
        final long[] acc = new long[] { 0L, 0L };
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    acc[1] += 1;
                    acc[0] += Files.size(p);
                } else if (Files.isDirectory(p)) {
                    long[] sub = directorySizeAndCount(p);
                    if (sub != null) {
                        acc[0] += sub[0];
                        acc[1] += sub[1];
                    }
                }
            }
        }
        return acc;
    }

    // Simple holder for DB credential row
    private static class DbCred {
        String credentialId;
        long id;
        String filename;
        long fileSize;
        long userId;

        DbCred(String credentialId, long id, String filename, long fileSize, long userId) {
            this.credentialId = credentialId;
            this.id = id;
            this.filename = filename;
            this.fileSize = fileSize;
            this.userId = userId;
        }
    }

    /**
     * Try multiple common method names via reflection to extract a text payload
     * from a ledger block.
     * Falls back to toString() if no method is present.
     */
    private static String extractTextFromLedgerBlock(com.vaultify.models.LedgerBlock block) {
        if (block == null)
            return null;

        String[] candidates = { "getAction", "getActionName", "getPayload", "getData", "getBody", "getDetails",
                "getMeta", "getContent", "getMessage" };

        for (String mname : candidates) {
            try {
                java.lang.reflect.Method m = block.getClass().getMethod(mname);
                if (m != null) {
                    Object val = m.invoke(block);
                    if (val != null) {
                        String s = val.toString().trim();
                        if (!s.isEmpty())
                            return s;
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // try next
            } catch (Throwable t) {
                // continue defensively
            }
        }

        try {
            String s = block.toString();
            return (s != null) ? s.trim() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Return true if the given column exists for the table (uses JDBC metadata).
     */
    private static boolean columnExists(java.sql.Connection conn, String table, String column) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, "public", table, column)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ===========================
    // DEV MODE COMMANDS
    // ===========================

    /**
     * Shows current development mode status and configuration.
     */
    private static void showDevModeStatus() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║           DEVELOPMENT MODE STATUS                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");

        boolean devMode = Config.isDevMode();
        System.out.println("\nCurrent Mode: " + (devMode ? "🔧 DEVELOPMENT" : "🔒 PRODUCTION"));
        System.out.println("Config Source: config.properties");
        System.out.println("Setting: dev.mode=" + devMode);

        if (devMode) {
            System.out.println("\n⚠️  DEVELOPMENT MODE ENABLED");
            System.out.println("\nAvailable dev commands:");
            System.out.println("  • test-db          - Test database connectivity");
            System.out.println("  • test-ledger      - Test remote ledger server");
            System.out.println("  • reset-all        - ⚠️  DESTRUCTIVE: Delete all data");
            System.out.println("  • dev-mode         - Show this status");
            System.out.println("\n⚠️  WARNING: reset-all will PERMANENTLY delete:");
            System.out.println("  - All database tables (users, credentials, tokens)");
            System.out.println("  - All vault storage files");
            System.out.println("  - All keys and certificates");
            System.out.println("  - Ledger data (ledger.json)");
        } else {
            System.out.println("\n✓ Production mode - dev commands are disabled");
            System.out.println("\nTo enable dev mode, set 'dev.mode=true' in config.properties");
        }
        System.out.println();
    }

    /**
     * DESTRUCTIVE: Resets entire system to initial state.
     * Deletes all database records, files, keys, and ledger data.
     * Requires explicit confirmation.
     */
    private static void resetAll(Scanner scanner) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                ⚠️  DANGER ZONE ⚠️                       ║");
        System.out.println("║           COMPLETE SYSTEM RESET                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");

        System.out.println("\n⚠️  This will PERMANENTLY delete:");
        System.out.println("  ✗ All user accounts");
        System.out.println("  ✗ All credentials and encrypted files");
        System.out.println("  ✗ All access tokens");
        System.out.println("  ✗ All keys (public/private)");
        System.out.println("  ✗ All certificates");
        System.out.println("  ✗ All ledger audit trail");
        System.out.println("  ✗ All file storage data");

        System.out.print("\nType 'DELETE' (uppercase) to confirm: ");
        String confirmation = scanner.nextLine().trim();

        if (!"DELETE".equals(confirmation)) {
            System.out.println("✗ Reset cancelled.");
            return;
        }

        System.out.print("\nAre you absolutely sure? Type 'YES' to proceed: ");
        String secondConfirm = scanner.nextLine().trim();

        if (!"YES".equals(secondConfirm)) {
            System.out.println("✗ Reset cancelled.");
            return;
        }

        System.out.println("\n🔄 Starting system reset...\n");

        int errors = 0;

        // 1. Reset Database
        System.out.println("[1/4] Resetting database tables...");
        try {
            java.sql.Connection conn = com.vaultify.db.Database.getConnection();

            // Disable foreign key checks temporarily
            try (PreparedStatement stmt = conn.prepareStatement("SET CONSTRAINTS ALL DEFERRED")) {
                stmt.execute();
            } catch (SQLException e) {
                // Try alternative for PostgreSQL
                try (PreparedStatement stmt = conn.prepareStatement("SET session_replication_role = 'replica'")) {
                    stmt.execute();
                }
            }

            // Delete in order to respect foreign keys
            String[] tables = { "tokens", "credentials", "users" };
            for (String table : tables) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table)) {
                    int deleted = stmt.executeUpdate();
                    System.out.println("  ✓ Deleted " + deleted + " rows from " + table);
                } catch (SQLException e) {
                    System.out.println("  ✗ Failed to clear " + table + ": " + e.getMessage());
                    errors++;
                }
            }

            // Re-enable foreign key checks
            try (PreparedStatement stmt = conn.prepareStatement("SET session_replication_role = 'origin'")) {
                stmt.execute();
            } catch (SQLException ignored) {
            }

            conn.close();
            System.out.println("  ✓ Database reset complete");

        } catch (Exception e) {
            System.out.println("  ✗ Database reset failed: " + e.getMessage());
            errors++;
        }

        // 2. Delete File Storage
        System.out.println("\n[2/4] Clearing file storage...");
        Path vaultData = Paths.get("vault_data");
        String[] directories = { "credentials", "db/credentials", "db/tokens", "db/users",
                "keys", "certificates" };

        for (String dir : directories) {
            Path dirPath = vaultData.resolve(dir);
            try {
                if (Files.exists(dirPath)) {
                    int deleted = deleteDirectoryContents(dirPath);
                    System.out.println("  ✓ Deleted " + deleted + " files from " + dir);
                }
            } catch (IOException e) {
                System.out.println("  ✗ Failed to clear " + dir + ": " + e.getMessage());
                errors++;
            }
        }

        // 3. Reset Ledger
        System.out.println("\n[3/4] Resetting ledger...");
        Path ledgerFile = Paths.get("ledger-server/data/ledger.json");
        try {
            if (Files.exists(ledgerFile)) {
                // Write empty array to reset ledger
                Files.writeString(ledgerFile, "[]", StandardCharsets.UTF_8);
                System.out.println("  ✓ Ledger reset to empty state");
            } else {
                System.out.println("  ℹ Ledger file not found (already empty)");
            }
        } catch (IOException e) {
            System.out.println("  ✗ Failed to reset ledger: " + e.getMessage());
            errors++;
        }

        // 4. Clear session
        System.out.println("\n[4/4] Clearing session...");
        try {
            authService.logout();
            System.out.println("  ✓ Session cleared");
        } catch (Exception e) {
            System.out.println("  ✗ Failed to clear session: " + e.getMessage());
            errors++;
        }

        // Final report
        System.out.println("\n" + "=".repeat(56));
        if (errors == 0) {
            System.out.println("✓ SYSTEM RESET COMPLETE");
            System.out.println("  All data has been permanently deleted.");
            System.out.println("  The system is now in initial state.");
        } else {
            System.out.println("⚠️  SYSTEM RESET COMPLETED WITH " + errors + " ERROR(S)");
            System.out.println("  Some data may not have been deleted.");
            System.out.println("  Check error messages above for details.");
        }
        System.out.println("=".repeat(56) + "\n");
    }

    /**
     * Helper: Delete all contents of a directory without deleting the directory
     * itself.
     * Returns count of files deleted.
     */
    private static int deleteDirectoryContents(Path dir) throws IOException {
        int count = 0;
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    count += deleteDirectoryContents(entry);
                    Files.delete(entry);
                } else {
                    Files.delete(entry);
                    count++;
                }
            }
        }
        return count;
    }
}
