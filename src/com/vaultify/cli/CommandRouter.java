package com.vaultify.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.List;
import java.util.Scanner;

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
 */
public class CommandRouter {

    // Services
    private static final AuthService authService = new AuthService();
    private static final VerificationService verificationService = new VerificationService();
    private static final VaultService vaultService = new VaultService();
    private static final TokenService tokenService = new TokenService();
    private static final LedgerService ledgerService = new LedgerService();

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
            case "revoke-token" -> revokeToken(scanner);
            case "list-tokens" -> listTokens();
            case "verify-ledger" -> verifyLedger();
            case "test-ledger" -> testLedgerConnection();
            case "test-db" -> testDatabaseConnection();
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

            // Get user's public key path for certificate
            Path publicKeyPath = Paths.get("vault_data/keys/" + user.getUsername() + "_public.pem");
            if (!Files.exists(publicKeyPath)) {
                System.out.println("✗ Public key not found: " + publicKeyPath);
                return;
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
            try (java.sql.ResultSet rs = conn.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
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
            String[] expectedTables = {"users", "credentials", "tokens"};
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
        System.out.println("\n================================");
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
                System.out.println("✓ Latest block index: " + latest.getIndex());
                System.out.println("✓ Latest block action: " + latest.getAction());
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
        System.out.println("Available commands:");
        System.out.println("  register       - create a new user with RSA key pair");
        System.out.println("  login          - login with username/password");
        System.out.println("  logout         - logout current user");
        System.out.println("  whoami         - show current logged-in user");
        System.out.println("  vault          - vault operations (add/list/view/delete credentials)");
        System.out.println("  revoke-token   - revoke a previously generated token");
        System.out.println("  list-tokens    - list all tokens you've generated");
        System.out.println("  verify-ledger  - verify integrity of the blockchain ledger");
        System.out.println("  test-ledger    - test connection to remote ledger server");
        System.out.println("  test-db        - test database connection and schema");
        System.out.println("  help           - show this help");
        System.out.println("  exit           - quit CLI");
    }
}
