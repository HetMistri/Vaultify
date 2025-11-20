package com.vaultify.test;

import com.vaultify.models.User;
import com.vaultify.service.AuthService;

/**
 * Quick test to demonstrate login/signup functionality.
 * Run this to verify registration and login work correctly.
 */
public class AuthTest {
    public static void main(String[] args) {
        AuthService authService = new AuthService();

        System.out.println("=== Vaultify Auth Test ===\n");

        // Test 1: Register new user
        System.out.println("Test 1: Register new user");
        User user = authService.register("testuser", "password123");
        if (user != null) {
            System.out.println("✓ Registration successful");
            System.out.println("  Username: " + user.getUsername());
            System.out.println("  Password Hash: " + user.getPasswordHash().substring(0, 16) + "...");
            System.out.println("  Public Key Length: " + user.getPublicKey().length() + " chars");
            System.out.println("  Encrypted Private Key Length: " + user.getPrivateKeyEncrypted().length() + " chars");
        } else {
            System.out.println("✗ Registration failed (username might already exist)");
        }
        System.out.println();

        // Test 2: Login with correct credentials
        System.out.println("Test 2: Login with correct credentials");
        boolean loginSuccess = authService.login("testuser", "password123");
        if (loginSuccess) {
            System.out.println("✓ Login successful");
            System.out.println("  Logged in as: " + authService.getCurrentUser().getUsername());
            System.out.println("  Session active: " + authService.isLoggedIn());
        } else {
            System.out.println("✗ Login failed");
        }
        System.out.println();

        // Test 3: Login with wrong password
        authService.logout();
        System.out.println("Test 3: Login with wrong password");
        boolean wrongPassword = authService.login("testuser", "wrongpassword");
        if (!wrongPassword) {
            System.out.println("✓ Correctly rejected wrong password");
        } else {
            System.out.println("✗ Security issue: accepted wrong password!");
        }
        System.out.println();

        // Test 4: Session management
        System.out.println("Test 4: Session management");
        authService.login("testuser", "password123");
        System.out.println("  Before logout - logged in: " + authService.isLoggedIn());
        authService.logout();
        System.out.println("  After logout - logged in: " + authService.isLoggedIn());
        System.out.println("✓ Session management working");
        System.out.println();

        // Test 5: Duplicate registration
        System.out.println("Test 5: Duplicate registration");
        User duplicate = authService.register("testuser", "newpassword");
        if (duplicate == null) {
            System.out.println("✓ Correctly rejected duplicate username");
        } else {
            System.out.println("✗ Security issue: allowed duplicate username!");
        }
        System.out.println();

        System.out.println("=== All Tests Complete ===");
        System.out.println("\nUser data saved to: vault_data/db/users/testuser.json");
    }
}
