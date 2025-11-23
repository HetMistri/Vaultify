package com.vaultify.dao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultify.models.User;

public class FileUserDAO {
    private static final String USER_DIR = "vault_data/db/users";
    private final Gson gson;

    public FileUserDAO() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(Paths.get(USER_DIR));
        } catch (IOException e) {
            System.err.println("File error while creating user directory: " + e.getMessage());
        }
    }

    public void save(User user) {
        // Filename is username.json to ensure uniqueness
        String filename = user.getUsername() + ".json";
        try (FileWriter writer = new FileWriter(Paths.get(USER_DIR, filename).toFile())) {
            gson.toJson(user, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user to file", e);
        }
    }

    public User findByUsername(String username) {
        File file = Paths.get(USER_DIR, username + ".json").toFile();
        if (!file.exists())
            return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, User.class);
        } catch (IOException e) {
            System.err.println("File error while loading user: " + e.getMessage());
        }
        return null;
    }
}