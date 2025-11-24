# Vaultify Project Inconsistencies & Disconnected Components

**Generated:** November 22, 2025  
**Analysis Type:** Deep System Scan

---

## Executive Summary

The project has **4 major architectural layers implemented**, but several components remain **disconnected or contain placeholder logic**. This document categorizes all identified issues by severity and provides implementation recommendations.

---

## 🔴 CRITICAL ISSUES (Breaking Functionality)

### 1. **JdbcCredentialDAO Missing Delete Method**

**Location:** `src/com/vaultify/dao/JdbcCredentialDAO.java`  
**Issue:**

- `VaultService.deleteCredential()` tries to delete from database but method doesn't exist
- Only `FileCredentialDAO` has `delete(String id)` method
- Database records become orphaned when credentials are deleted

**Impact:** Dual storage inconsistency, database bloat  
**Fix Required:**

```java
public void delete(String credentialId) {
    String sql = "DELETE FROM credentials WHERE filepath = ?";
    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "vault_data/credentials/" + credentialId + ".bin");
        pstmt.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException("Error deleting credential from DB", e);
    }
}
```

---

### 2. **Token Revocation Not Persisted**

**Location:** `src/com/vaultify/service/TokenService.java`  
**Issue:**

- `revokedTokens` is in-memory `HashSet`
- Revocations lost on restart
- No database persistence for revoked tokens

**Impact:** Security vulnerability - revoked tokens become valid after restart  
**Fix Required:**

- Add `revoked` boolean column to `tokens` table
- Update `JdbcTokenDAO` to support revocation persistence
- Load revoked tokens on startup

---

### 3. **Missing CredentialMetadata Fields in Database Schema**

**Location:** `docker/init-db.sql` vs `models/CredentialMetadata.java`  
**Issue:**

- Database `credentials` table has: `id, user_id, filename, filepath, metadata, created_at`
- `CredentialMetadata` model has: `credentialIdString, dataHash, fileSize, encryptedKeyBase64, ivBase64, userId, timestamp`
- **Critical missing fields:** `encryptedKeyBase64`, `ivBase64` (needed for decryption!)

**Current Workaround:** Stored in `metadata` TEXT column as string parsing (`"hash:xyz;size:123"`)  
**Impact:**

- Cannot decrypt credentials retrieved from database (missing encrypted key & IV)
- Data integrity issues with string parsing

**Fix Required:**

```sql
ALTER TABLE credentials ADD COLUMN encrypted_key TEXT;
ALTER TABLE credentials ADD COLUMN iv TEXT;
ALTER TABLE credentials ADD COLUMN data_hash TEXT;
```

Update `JdbcCredentialDAO.save()` and `findByUserId()` to properly map these fields.

---

## 🟠 HIGH PRIORITY (Partially Implemented)

### 4. **CLI Verify Token Command is Placeholder**

**Location:** `src/com/vaultify/cli/CommandRouter.java:425-436`  
**Issue:**

```java
private static void verifyToken(Scanner scanner) {
    System.out.print("Enter share token: ");
    String token = scanner.nextLine().trim();
    System.out.print("Enter certificate path: ");
    String certPath = scanner.nextLine().trim();
    System.out.println("Verification subsystem not implemented in this demo.");
}
```

**Available but Not Connected:**

- `VerificationService.verifyCertificate()` EXISTS and is functional
- `CertificateVerifier` has complete implementation
- Just needs CLI wiring

**Fix Required:**

```java
private static void verifyToken(Scanner scanner) {
    System.out.print("Enter share token: ");
    String token = scanner.nextLine().trim();
    System.out.print("Enter certificate path: ");
    String certPath = scanner.nextLine().trim();

    try {
        System.out.print("Enter issuer PUBLIC key path: ");
        Path pubKeyPath = Paths.get(scanner.nextLine().trim());

        CertificateVerifier.Result result = verificationService.verifyCertificate(
            Paths.get(certPath), pubKeyPath);

        System.out.println("\n=== Verification Result ===");
        System.out.println("Valid: " + result.valid);
        System.out.println("Message: " + result.message);
        System.out.println("===========================");
    } catch (Exception e) {
        System.out.println("✗ Verification failed: " + e.getMessage());
    }
}
```

---

### 5. **CLI Verify-Ledger Command is Stub**

**Location:** `src/com/vaultify/cli/CommandRouter.java:442`  
**Issue:**

```java
private static void verifyLedger() {
    System.out.println("Ledger verification not implemented in this demo.");
}
```

**Available but Not Connected:**

- `LedgerService.verifyIntegrity()` EXISTS and is functional
- `LedgerEngine` has complete chain verification

**Fix Required:**

```java
private static void verifyLedger() {
    try {
        LedgerService ledgerService = new LedgerService();
        List<String> errors = ledgerService.verifyIntegrity();

        if (errors.isEmpty()) {
            System.out.println("✓ Ledger integrity verified - no issues found");
            System.out.println("  Total blocks: " + ledgerService.getChain().size());
        } else {
            System.out.println("✗ Ledger integrity check FAILED:");
            errors.forEach(err -> System.out.println("  - " + err));
        }
    } catch (Exception e) {
        System.out.println("✗ Verification error: " + e.getMessage());
    }
}
```

---

### 6. **Token Expiry Cleanup Not Scheduled**

**Location:** `src/com/vaultify/threading/TokenExpiryScheduler.java`  
**Issue:**

- Scheduler exists but no cleanup task is ever scheduled
- `TokenExpiryScheduler.scheduleTokenCleanup()` is never called
- Expired tokens accumulate in database/files

**Fix Required:**

- Create `TokenCleanupTask` runnable
- Schedule in `VaultifyApplication.main()`:

```java
// In VaultifyApplication
TokenExpiryScheduler.scheduleTokenCleanup(
    new TokenCleanupTask(),
    0,  // Initial delay
    3600,  // Run every hour
    TimeUnit.SECONDS
);
```

---

## 🟡 MEDIUM PRIORITY (Missing Implementations)

### 7. **Threading Components are Empty Skeletons**

**Location:** `src/com/vaultify/threading/`  
**Issues:**

#### ActivityLogger.java

```java
public void run() {
    // TODO: write activity logs asynchronously
}
```

- Never instantiated or used
- No log file path configured
- No log format defined

#### EncryptionTask.java

```java
public byte[] call() throws Exception {
    // TODO: perform encryption and return ciphertext
    return new byte[0];
}
```

- Never used (encryption is synchronous in `CredentialFileManager`)
- Could be used for batch encryption

#### LedgerWriter.java

```java
public void run() {
    // TODO: append a block to the ledger using a service/engine
}
```

- Never used (ledger writes use `ThreadManager.runAsync()` with inline lambda)
- Redundant with current async approach

**Recommendation:**

- **ActivityLogger:** Implement for audit logging (user actions, failures)
- **EncryptionTask:** Use for batch operations or keep current sync approach
- **LedgerWriter:** Either implement or remove (current approach works)

---

### 8. **UserService is Redundant Wrapper**

**Location:** `src/com/vaultify/service/UserService.java`  
**Issue:**

- Every method just delegates to `AuthService`
- No additional business logic
- Not used anywhere in codebase (CLI uses `AuthService` directly)

**Recommendation:**

- Either:
  1. Remove `UserService` entirely (current usage pattern)
  2. Move user management logic (list users, delete user, update profile) here

---

### 9. **Credential Model Unused**

**Location:** `src/com/vaultify/models/Credential.java`  
**Issue:**

- Complete POJO with getters/setters matching DB schema
- Never instantiated or used in codebase
- `CredentialMetadata` is used instead (different field structure)

**Recommendation:**

- Unify models: Either use `Credential` everywhere OR remove it
- Current `CredentialMetadata` has encryption fields not in `Credential`

---

### 10. **CredentialDAO Interface Not Used**

**Location:** `src/com/vaultify/dao/CredentialDAO.java`  
**Issue:**

- Defines interface for credential operations
- `FileCredentialDAO` doesn't implement it
- `JdbcCredentialDAO` doesn't implement it
- Interface has method `deleteCredential(long id)` but implementations use `delete(String uuid)`

**Recommendation:**

- Update interface to match actual implementations:

```java
public interface CredentialDAO {
    void save(CredentialMetadata meta);
    CredentialMetadata findById(String credentialId);
    List<CredentialMetadata> findByUserId(long userId);
    void delete(String credentialId);
}
```

- Have both DAOs implement this interface

---

## 🟢 LOW PRIORITY (Enhancements)

### 11. **Missing Database Connection Pooling**

**Location:** `src/com/vaultify/db/Database.java`  
**Issue:**

- Creates new connection for every operation
- No connection pooling (HikariCP, C3P0, etc.)

**Impact:** Performance degradation under load  
**Recommendation:** Add HikariCP for production use

---

### 12. **No Transaction Management**

**Location:** All DAO operations  
**Issue:**

- Dual storage operations (file + DB) not atomic
- If DB write fails, file is already written (inconsistency)

**Recommendation:** Implement transaction coordinator or accept eventual consistency

---

### 13. **Missing Input Validation in DAOs**

**Location:** All DAO classes  
**Issue:**

- No null checks
- No constraint validation
- Relies on database constraints

**Recommendation:** Add validation layer before database operations

---

### 14. **Password Hashing is Weak**

**Location:** `src/com/vaultify/service/AuthService.java`  
**Issue:**

- Uses plain SHA-256 (no salt, no iterations)
- Vulnerable to rainbow table attacks

**Current:**

```java
String passwordHash = HashUtil.sha256(password);
```

**Recommendation:**

```java
// Use BCrypt, Argon2, or PBKDF2
String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
```

---

### 15. **No Graceful Shutdown Hook**

**Location:** `src/com/vaultify/app/VaultifyApplication.java`  
**Issue:**

- ThreadManager executors not shutdown on exit
- Could leave threads running

**Recommendation:**

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    ThreadManager.shutdown();
    System.out.println("Vaultify shutdown complete");
}));
```

---

## 📊 Summary Statistics

| Category                   | Count  | Severity |
| -------------------------- | ------ | -------- |
| Critical (Breaking)        | 3      | 🔴       |
| High Priority (Partial)    | 4      | 🟠       |
| Medium Priority (Missing)  | 6      | 🟡       |
| Low Priority (Enhancement) | 5      | 🟢       |
| **TOTAL**                  | **18** |          |

---

## 🎯 Recommended Action Plan

### Phase 1: Critical Fixes (1-2 hours)

1. ✅ Add `JdbcCredentialDAO.delete()` method
2. ✅ Fix database schema - add `encrypted_key`, `iv`, `data_hash` columns
3. ✅ Update `JdbcCredentialDAO` to store/retrieve encryption fields
4. ✅ Implement token revocation persistence

### Phase 2: High Priority Connections (1 hour)

5. ✅ Wire up `verify` command to existing `VerificationService`
6. ✅ Wire up `verify-ledger` command to existing `LedgerService`
7. ✅ Schedule token expiry cleanup task

### Phase 3: Medium Priority Cleanup (2-3 hours)

8. Implement `ActivityLogger` for audit trail
9. Unify credential models (`Credential` vs `CredentialMetadata`)
10. Update `CredentialDAO` interface and implementations
11. Decision on `UserService` (remove or enhance)

### Phase 4: Low Priority Enhancements (Optional)

12. Add connection pooling
13. Implement transaction management
14. Upgrade password hashing to BCrypt/Argon2
15. Add shutdown hooks

---

## 🔍 Testing Recommendations

### Unit Tests Needed

- [ ] DAO layer CRUD operations (file + JDBC)
- [ ] Crypto operations (encrypt/decrypt round-trip)
- [ ] Ledger integrity verification
- [ ] Token validation and revocation
- [ ] Password hashing verification

### Integration Tests Needed

- [ ] End-to-end credential add/retrieve/delete
- [ ] Dual storage consistency verification
- [ ] Certificate generation and verification
- [ ] Database schema migration validation

---

## 📝 Notes

- Most inconsistencies are **wiring issues** rather than missing implementations
- Core crypto, ledger, and verification services **are complete**
- Database schema needs **one-time migration** for existing users
- Threading skeletons can be **implemented or removed** based on requirements

---

**Next Steps:** Prioritize Phase 1 critical fixes to ensure data integrity and functional completeness.
