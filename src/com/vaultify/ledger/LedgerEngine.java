package com.vaultify.ledger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.vaultify.crypto.HashUtil;
import com.vaultify.util.Config;

/**
 * LedgerEngine:
 * - Maintains the in-memory ledger (List<LedgerBlock>)
 * - Persists ledger to disk (JSON)
 * - Provides addBlock(...) and verifyIntegrity()
 * Thread-safe: public methods are synchronized to avoid concurrent writes.
 */
public class LedgerEngine {
    private final File ledgerFile;
    private final Gson gson;
    private final List<LedgerBlock> chain;

    public LedgerEngine() {
        String path = Config.get("ledger.file");
        if (path == null || path.isEmpty()) path = "./vault_data/ledger.json";
        this.ledgerFile = new File(path);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.chain = new ArrayList<>();
        load();
    }

    /**
     * Load the ledger file. If missing, initialize genesis block.
     */
    private synchronized void load() {
        try {
            if (!ledgerFile.exists() || ledgerFile.length() == 0) {
                // create parent dirs and empty file
                File parent = ledgerFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                writeToDisk(); // will write an empty list -> we add genesis next
                createGenesisIfNeeded();
                return;
            }

            try (FileReader fr = new FileReader(ledgerFile, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<LedgerBlock>>() {
                }.getType();
                List<LedgerBlock> loaded = gson.fromJson(fr, listType);
                if (loaded != null) {
                    chain.clear();
                    chain.addAll(loaded);
                }
            }
            createGenesisIfNeeded();
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            throw new RuntimeException("Failed to load ledger file: " + ledgerFile.getAbsolutePath(), e);
        }
    }

    private synchronized void createGenesisIfNeeded() {
        if (chain.isEmpty()) {
            long ts = System.currentTimeMillis();
            String genesisDataHash = HashUtil.sha256("GENESIS");
            String genesisHash = computeHash(0, ts, "GENESIS", genesisDataHash, "0");
            LedgerBlock genesis = new LedgerBlock(0, ts, "GENESIS", genesisDataHash, "0", genesisHash);
            chain.add(genesis);
            writeToDisk();
        }
    }

    /**
     * Add a new block to the ledger and persist it.
     * action: short action string
     * dataHash: SHA256 hex string describing the payload (credential id + metadata)
     */
    public synchronized LedgerBlock addBlock(String action, String dataHash) {
        int nextIndex = chain.size();
        long ts = System.currentTimeMillis();
        String prevHash = chain.getLast().getHash();
        String hash = computeHash(nextIndex, ts, action, dataHash, prevHash);

        LedgerBlock block = new LedgerBlock(nextIndex, ts, action, dataHash, prevHash, hash);
        chain.add(block);
        writeToDisk();
        return block;
    }

    /**
     * Compute the block hash deterministically.
     */
    private String computeHash(int index, long timestamp, String action, String dataHash, String prevHash) {
        String combined = index + "|" + timestamp + "|" + action + "|" + dataHash + "|" + prevHash;
        return HashUtil.sha256(combined);
    }

    /**
     * Verify the entire chain. Returns an immutable list of error messages.
     * If empty => ledger valid.
     */
    public synchronized List<String> verifyIntegrity() {
        List<String> errors = new ArrayList<>();

        if (chain.isEmpty()) {
            errors.add("Ledger is empty");
            return errors;
        }

        for (int i = 0; i < chain.size(); i++) {
            LedgerBlock blk = chain.get(i);

            // 1) verify prevHash linkage
            if (i == 0) {
                if (!"0".equals(blk.getPrevHash())) {
                    errors.add("Genesis prevHash must be '0' but was: " + blk.getPrevHash());
                }
            } else {
                String expectedPrev = chain.get(i - 1).getHash();
                if (!expectedPrev.equals(blk.getPrevHash())) {
                    errors.add(String.format("Block %d prevHash mismatch. Expected=%s Found=%s", i, expectedPrev, blk.getPrevHash()));
                }
            }

            // 2) verify hash correctness
            String recomputed = computeHash(blk.getIndex(), blk.getTimestamp(), blk.getAction(), blk.getDataHash(), blk.getPrevHash());
            if (!recomputed.equals(blk.getHash())) {
                errors.add(String.format("Block %d hash mismatch. Expected=%s Found=%s", i, recomputed, blk.getHash()));
            }
        }

        return Collections.unmodifiableList(errors);
    }

    public synchronized List<LedgerBlock> getChain() {
        return List.copyOf(chain);
    }

    private synchronized void writeToDisk() {
        try (FileWriter fw = new FileWriter(ledgerFile, StandardCharsets.UTF_8)) {
            gson.toJson(chain, fw);
            fw.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist ledger to disk: " + ledgerFile.getAbsolutePath(), e);
        }
    }
}
