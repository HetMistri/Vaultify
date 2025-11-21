package com.vaultify.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultify.models.Token;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileTokenDAO {
    private static final String TOKEN_DIR = "vault_data/db/tokens";
    private final Gson gson;

    public FileTokenDAO() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(Paths.get(TOKEN_DIR));
        } catch (IOException e) {
            e.printStackTrace();
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
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, Token.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}