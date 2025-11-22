package com.vaultify.dao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultify.models.Token;

public class FileTokenDAO {
    private static final String TOKEN_DIR = "vault_data/db/tokens";
    private final Gson gson;

    public FileTokenDAO() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(Paths.get(TOKEN_DIR));
        } catch (IOException e) {
            System.err.println("File error while creating token directory: " + e.getMessage());
        }
    }

    public void save(Token token) {
        String filename = token.getToken() + ".json";
        try (FileWriter writer = new FileWriter(Paths.get(TOKEN_DIR, filename).toFile())) {
            gson.toJson(token, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }

    public Token findByToken(String tokenString) {
        File file = Paths.get(TOKEN_DIR, tokenString + ".json").toFile();
        if (!file.exists())
            return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, Token.class);
        } catch (IOException e) {
            System.err.println("File error while loading token: " + e.getMessage());
        }
        return null;
    }

    public java.util.List<Token> findAll() {
        java.util.List<Token> tokens = new java.util.ArrayList<>();
        File dir = new File(TOKEN_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File f : files) {
                try (FileReader reader = new FileReader(f)) {
                    Token token = gson.fromJson(reader, Token.class);
                    if (token != null) {
                        tokens.add(token);
                    }
                } catch (IOException e) {
                    System.err.println("Warning: Failed to read token file " + f.getName());
                }
            }
        }
        return tokens;
    }
}