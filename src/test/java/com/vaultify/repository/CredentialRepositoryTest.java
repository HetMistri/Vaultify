package com.vaultify.repository;

import com.vaultify.models.CredentialMetadata;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialRepositoryTest {

    @Test
    void testRepositoryLifecycle() {
        // Use the factory to get the configured repository (defaults to Dual or
        // whatever is in config)
        // For testing, we might want to force a specific one, but testing the factory
        // default is also good.
        CredentialRepository repository = RepositoryFactory.get().credentialRepository();

        long userId = 9999L; // Test user ID
        String credentialId = UUID.randomUUID().toString();

        CredentialMetadata meta = new CredentialMetadata();
        meta.credentialIdString = credentialId;
        meta.userId = userId;
        meta.filename = "test-secret.txt";
        meta.dataHash = "dummy-hash";
        meta.encryptedKeyBase64 = "dummy-key";
        meta.ivBase64 = "dummy-iv";
        meta.fileSize = 123;
        meta.timestamp = System.currentTimeMillis();

        // 1. Save
        repository.save(meta, userId);
        System.out.println("Saved credential: " + credentialId);

        // 2. Find by ID
        CredentialMetadata retrieved = repository.findByCredentialId(credentialId);
        assertNotNull(retrieved, "Should retrieve the saved credential");
        assertEquals(credentialId, retrieved.credentialIdString);
        assertEquals(userId, retrieved.userId);

        // 3. Find by User ID
        List<CredentialMetadata> userCredentials = repository.findByUserId(userId);
        assertFalse(userCredentials.isEmpty(), "Should find credentials for user");
        boolean found = userCredentials.stream().anyMatch(c -> c.credentialIdString.equals(credentialId));
        assertTrue(found, "List should contain the saved credential");

        // 4. Delete
        repository.deleteByCredentialId(credentialId);
        System.out.println("Deleted credential: " + credentialId);

        // 5. Verify Deletion
        CredentialMetadata deleted = repository.findByCredentialId(credentialId);
        assertNull(deleted, "Credential should be null after deletion");
    }
}
