package com.vaultify.repository;

import com.vaultify.models.CredentialMetadata;
import com.vaultify.models.User;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialRepositoryTest {

    @Test
    void testRepositoryLifecycle() {
        // Use the factory to get the configured repository
        CredentialRepository repository = RepositoryFactory.get().credentialRepository();
        UserRepository userRepository = RepositoryFactory.get().userRepository();

        // Create a test user first to satisfy foreign key constraints
        String testUsername = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(testUsername);
        user.setPasswordHash("dummyhash");
        user.setPublicKey("dummy-public-key");
        user.setPrivateKeyEncrypted("dummy-private-key");
        user = userRepository.save(user);
        assertNotNull(user, "User should be saved");
        long userId = user.getId();

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

        try {
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
        } finally {
            // Cleanup user
            userRepository.delete(userId);
        }
    }
}
