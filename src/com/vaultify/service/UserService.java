package com.vaultify.service;

import com.vaultify.dao.FileUserDAO;
import com.vaultify.models.User;

/**
 * UserService handles user management operations.
 * Delegates to AuthService for authentication, provides user lookup and
 * listing.
 */
public class UserService {
    private final FileUserDAO userDAO;
    private final AuthService authService;

    public UserService() {
        this.userDAO = new FileUserDAO();
        this.authService = new AuthService();
    }

    /**
     * Register a new user.
     * 
     * @param username Username
     * @param password Password
     * @return User object if successful, null if username exists
     */
    public User registerUser(String username, String password) {
        return authService.register(username, password);
    }

    /**
     * Login user.
     * 
     * @param username Username
     * @param password Password
     * @return true if successful
     */
    public boolean loginUser(String username, String password) {
        return authService.login(username, password);
    }

    /**
     * Find user by username.
     */
    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    /**
     * Get current logged-in user.
     */
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return authService.isLoggedIn();
    }

    /**
     * Logout current user.
     */
    public void logout() {
        authService.logout();
    }
}
