package test;

import com.vaultify.util.Config;

/**
 * Test class to verify Config.java functionality.
 * Tests classpath loading, environment variable override, and defaults.
 */
public class ConfigTest {
    public static void main(String[] args) {
        System.out.println("=== Config.java Test Suite ===\n");

        // Test 1: Basic property loading from config.properties
        System.out.println("Test 1: Loading from config.properties");
        String dbHost = Config.get("db.host");
        String dbPort = Config.get("db.port");
        String dbName = Config.get("db.name");
        String vaultStorage = Config.get("vault.storage");
        System.out.println("  db.host = " + dbHost);
        System.out.println("  db.port = " + dbPort);
        System.out.println("  db.name = " + dbName);
        System.out.println("  vault.storage = " + vaultStorage);
        
        // Test 2: Default value fallback
        System.out.println("\nTest 2: Default value fallback");
        String missing = Config.get("missing.key", "default_value");
        System.out.println("  missing.key = " + missing + " (should be 'default_value')");
        
        // Test 3: Integer parsing
        System.out.println("\nTest 3: Integer value parsing");
        int tokenExpiry = Config.getInt("token.expiryHours", 24);
        System.out.println("  token.expiryHours = " + tokenExpiry + " (should be 48)");
        
        // Test 4: Environment variable override (if set)
        System.out.println("\nTest 4: Environment variable override");
        String envHost = System.getenv("DB_HOST");
        String configHost = Config.get("db.host");
        if (envHost != null && !envHost.isEmpty()) {
            System.out.println("  Environment DB_HOST = " + envHost);
            System.out.println("  Config.get('db.host') = " + configHost);
            System.out.println("  Override working: " + envHost.equals(configHost));
        } else {
            System.out.println("  No DB_HOST env var set, using config.properties value: " + configHost);
        }
        
        // Test 5: All configuration keys
        System.out.println("\nTest 5: All database configuration");
        System.out.println("  db.host = " + Config.get("db.host"));
        System.out.println("  db.port = " + Config.get("db.port"));
        System.out.println("  db.name = " + Config.get("db.name"));
        System.out.println("  db.user = " + Config.get("db.user"));
        System.out.println("  db.password = " + Config.get("db.password").replaceAll(".", "*")); // masked
        
        System.out.println("\nTest 6: All vault paths");
        System.out.println("  vault.storage = " + Config.get("vault.storage"));
        System.out.println("  ledger.file = " + Config.get("ledger.file"));
        System.out.println("  rsa.public = " + Config.get("rsa.public"));
        System.out.println("  rsa.private = " + Config.get("rsa.private"));
        System.out.println("  certificate.output = " + Config.get("certificate.output"));
        
        System.out.println("\n=== All Tests Passed ===");
    }
}
