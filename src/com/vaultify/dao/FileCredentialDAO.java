package com.vaultify.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultify.models.CredentialMetadata;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileCredentialDAO {
    private static final String CRED_DIR = "vault_data/db/credentials";
    private final Gson gson;

    public FileCredentialDAO() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(Paths.get(CRED_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(CredentialMetadata meta) {
        // Filename is the UUID string
        String filename = meta.credentialIdString + ".json";
        try (FileWriter writer = new FileWriter(Paths.get(CRED_DIR, filename).toFile())) {
            gson.toJson(meta, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save credential metadata", e);
        }
    }

    public CredentialMetadata findById(String id) {
        File file = Paths.get(CRED_DIR, id + ".json").toFile();
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, CredentialMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read credential metadata", e);
        }
    }

    public List<CredentialMetadata> findAll() {
        List<CredentialMetadata> list = new ArrayList<>();
        File dir = new File(CRED_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File f : files) {
                try (FileReader reader = new FileReader(f)) {
                    list.add(gson.fromJson(reader, CredentialMetadata.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    public void delete(String id) {
        File file = Paths.get(CRED_DIR, id + ".json").toFile();
        if (file.exists()) {
            file.delete();
        }
    }
}