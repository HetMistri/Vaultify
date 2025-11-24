package com.vaultify.service;

import com.vaultify.models.User;
import com.vaultify.repository.RepositoryFactory;
import com.vaultify.repository.UserRepository;

/**
 * UserService handles user management operations.
 * Delegates to AuthService for authentication, provides user lookup and
 * listing.
 */
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService() {
        this.userRepository = RepositoryFactory.get().userRepository();
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
        return userRepository.findByUsername(username);
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
