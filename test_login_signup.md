# Login and Signup Implementation Test Guide

## Implementation Summary

The login and signup functionality has been fully implemented with the following features:

### 1. **AuthService** - Complete Authentication System

- **Registration**:

  - RSA-2048 key pair generation for each user
  - Password hashing using SHA-256
  - Private key encryption using AES-256-GCM (encrypted with password-derived key)
  - Public key stored as Base64-encoded X.509 format
  - User data persisted to `vault_data/db/users/<username>.json`

- **Login**:

  - Password verification via SHA-256 hash comparison
  - Private key decryption using password
  - Session management with in-memory user context
  - Returns decrypted private key for vault operations

- **Session Management**:
  - `isLoggedIn()` - Check if user is authenticated
  - `getCurrentUser()` - Get logged-in user object
  - `getCurrentUserPrivateKey()` - Access decrypted private key
  - `getCurrentUserPublicKey()` - Access public key
  - `logout()` - Clear session

### 2. **UserService** - User Management

- Delegates authentication to AuthService
- Provides user lookup via `findByUsername()`
- Wraps registration: `registerUser(username, password)`
- Wraps login: `loginUser(username, password)`
- Session queries: `getCurrentUser()`, `isLoggedIn()`, `logout()`

### 3. **CommandRouter** - CLI Integration

- **New Commands**:

  - `register` - Create new user with password confirmation and RSA key generation
  - `login` - Authenticate user and establish session
  - `logout` - Clear current session
  - `whoami` - Display current logged-in user info

- **Enhanced Security**:
  - All vault operations require authentication
  - Session tracking prevents duplicate logins
  - Password confirmation during registration

## Testing the Implementation

### Manual Testing Steps

1. **Build the project**:

   ```bash
   .\gradlew.bat build -x test
   ```

2. **Run the application**:

   ```bash
   .\gradlew.bat run
   ```

3. **Test Registration**:

   ```
   vaultify> register
   Enter username: testuser
   Enter password: securepassword123
   Confirm password: securepassword123
   ```

   **Expected Output**:

   ```
   User 'testuser' registered successfully.
   RSA key pair generated and private key encrypted.
   ```

   **Verify**: Check `vault_data/db/users/testuser.json` exists with:

   - `username`: "testuser"
   - `passwordHash`: SHA-256 hash
   - `publicKey`: Base64-encoded RSA public key
   - `privateKeyEncrypted`: Base64-encoded encrypted private key (IV + ciphertext)

4. **Test Login**:

   ```
   vaultify> login
   Username: testuser
   Password: securepassword123
   ```

   **Expected Output**:

   ```
   Login successful. Welcome, testuser!
   ```

5. **Test Whoami**:

   ```
   vaultify> whoami
   ```

   **Expected Output**:

   ```
   Logged in as: testuser
   User ID: 0
   ```

6. **Test Vault Access Control**:

   ```
   vaultify> logout
   vaultify> vault
   ```

   **Expected Output**:

   ```
   User 'testuser' logged out successfully.
   Please login first to access vault.
   ```

7. **Test Invalid Login**:

   ```
   vaultify> login
   Username: testuser
   Password: wrongpassword
   ```

   **Expected Output**:

   ```
   Invalid credentials.
   ```

8. **Test Duplicate Registration**:

   ```
   vaultify> register
   Enter username: testuser
   Enter password: anypassword
   Confirm password: anypassword
   ```

   **Expected Output**:

   ```
   Username already exists.
   ```

### Automated Test Sequence

Create a test input file `test_input.txt`:

```
register
alice
password123
password123
login
alice
password123
whoami
vault
back
logout
login
alice
wrongpassword
login
alice
password123
whoami
exit
```

Then redirect input:

```bash
Get-Content test_input.txt | .\gradlew.bat run
```

## Security Features Implemented

1. **Password Security**:

   - SHA-256 hashing (not plaintext storage)
   - Password confirmation during registration
   - Constant-time comparison via hash matching

2. **Cryptographic Key Management**:

   - RSA-2048 key pair per user
   - Private key encrypted with AES-256-GCM
   - Password-derived AES key (SHA-256 hash truncated to 32 bytes)
   - IV prepended to ciphertext for proper decryption

3. **Session Management**:

   - In-memory session storage (cleared on logout)
   - Private key only in memory while logged in
   - Access control for sensitive operations (vault)

4. **Data Persistence**:
   - File-based storage via FileUserDAO
   - JSON serialization with Gson
   - User data stored in `vault_data/db/users/`

## Architecture Integration

```
CommandRouter (CLI)
    ↓
UserService (facade)
    ↓
AuthService (core auth logic)
    ↓
┌─────────────┬──────────────┬─────────────┐
│             │              │             │
FileUserDAO   HashUtil    AESEngine    RSAEngine
(persistence) (SHA-256)   (AES-256-GCM) (RSA-2048)
```

## Files Modified

1. **AuthService.java** - Complete rewrite with crypto integration
2. **UserService.java** - Real implementation replacing stubs
3. **CommandRouter.java** - Updated register/login, added logout/whoami, session checks
4. **FileUserDAO.java** - Already implemented (no changes needed)

## Next Steps

- ✅ Login/Signup: **COMPLETE**
- ⏳ Wire VaultService to CLI vault commands
- ⏳ Integration testing with real file encryption workflow
- ⏳ Add more robust error handling and input validation

## Build Status

```
BUILD SUCCESSFUL in 2s
6 actionable tasks: 5 executed, 1 up-to-date
```

All compilation successful with no errors.

## Verified Test Results

Run the automated test with:

```bash
.\gradlew.bat testAuth
```

**Test Output**:

```
=== Vaultify Auth Test ===

Test 1: Register new user
✓ Registration successful
  Username: testuser
  Password Hash: ef92b778bafe771e...
  Public Key Length: 392 chars
  Encrypted Private Key Length: 1664 chars

Test 2: Login with correct credentials
✓ Login successful
  Logged in as: testuser
  Session active: true

Test 3: Login with wrong password
✓ Correctly rejected wrong password

Test 4: Session management
  Before logout - logged in: true
  After logout - logged in: false
✓ Session management working

Test 5: Duplicate registration
✓ Correctly rejected duplicate username

=== All Tests Complete ===

User data saved to: vault_data/db/users/testuser.json
```

**All 5 tests passed!**

### User Data Verification

The generated user file (`vault_data/db/users/testuser.json`) contains:

- ✓ `username`: "testuser"
- ✓ `passwordHash`: "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f" (SHA-256)
- ✓ `publicKey`: 392 characters Base64-encoded RSA-2048 public key
- ✓ `privateKeyEncrypted`: 1664 characters Base64-encoded AES-256-GCM encrypted private key with IV
