# Vaultify - Quick Reference Guide

## 🚀 Getting Started

### Prerequisites
- Java 21
- Docker & Docker Compose
- Gradle 8.10+

### Initial Setup
```bash
# 1. Start PostgreSQL database
docker compose up -d

# 2. Build project
./gradlew build

# 3. Run application
./gradlew runLocal
```

---

## 📋 CLI Commands Reference

### User Management
| Command | Description | Example |
|---------|-------------|---------|
| `register` | Create new user with RSA keys | Prompts for username/password |
| `login` | Authenticate user | Prompts for credentials |
| `logout` | End current session | No arguments |
| `whoami` | Show current user | Displays username & ID |

### Vault Operations
| Command | Description | Usage |
|---------|-------------|-------|
| `vault` | Enter vault submenu | Interactive mode |
| `vault > add` | Add credential (interactive) | Choose file or text |
| `vault > add --file <path>` | Add from file | `add --file passwords.txt` |
| `vault > add --text` | Add text/password | Multi-line input |
| `vault > list` | List all credentials | Shows ID, name, size, date |
| `vault > view <id>` | Decrypt & view credential | `view abc-123-def` |
| `vault > delete <id>` | Delete credential | `delete abc-123-def` |
| `vault > back` | Exit vault submenu | Return to main CLI |

### Token & Sharing
| Command | Description | Usage |
|---------|-------------|-------|
| `share` | Generate share token & certificate | Prompts for credential ID |
| `list-tokens` | List all generated tokens | Shows status (valid/revoked/expired) |
| `revoke-token` | Revoke a token | Prompts for token string |
| `verify-cert` | Verify certificate signature | Prompts for cert & public key paths |

### Ledger & Diagnostics
| Command | Description | Output |
|---------|-------------|--------|
| `verify-ledger` | Check blockchain integrity | Lists any errors found |
| `test-db` | Test database connection | Shows tables & schema |
| `help` | Display command list | All available commands |
| `exit` | Quit application | Graceful shutdown |

---

## 🔐 Security Features

### Encryption
- **AES-256-GCM** for file encryption
- **RSA-OAEP-SHA256** for key wrapping
- **SHA-256** for hashing

### Key Management
- RSA 2048-bit key pairs per user
- Private keys encrypted with password-derived key
- Public keys for credential sharing

### Audit Trail
All operations logged to blockchain ledger:
- Credential add/delete
- Token generation/revocation
- Certificate generation/validation

---

## 📁 File Structure

```
vault_data/
├── credentials/          # Encrypted credential files (.bin)
├── certificates/         # Share certificates (.json)
├── keys/                # User RSA keys
├── ledger.json          # Blockchain audit log
└── db/
    ├── users/           # User metadata (JSON)
    ├── credentials/     # Credential metadata (JSON)
    └── tokens/          # Token metadata (JSON)
```

---

## 💾 Database Schema

### Tables
- **users** - User accounts with encrypted private keys
- **credentials** - Credential metadata with encryption info
- **tokens** - Share tokens with revocation status

### Key Fields
- `encrypted_key` - RSA-wrapped AES key (Base64)
- `iv` - AES initialization vector (Base64)
- `data_hash` - SHA-256 of original file
- `revoked` - Token revocation flag
- `issuer_user_id` - Token creator reference

---

## 🔄 Typical Workflow

### 1. User Registration & Login
```
vaultify> register
Enter username: alice
Enter password: ****
✓ User 'alice' registered successfully

vaultify> login
Username: alice
Password: ****
✓ Login successful
```

### 2. Store Credentials
```
vaultify> vault
vaultify:vault> add --file mypasswords.txt
✓ Credential added successfully!
  ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### 3. Share with Another User
```
vaultify> share
Enter credential ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Expiry in hours: 24

✓ Share token generated!
  Token: 9f8e7d6c5b4a3210fedcba9876543210
  Certificate: vault_data/certificates/cert-9f8e7d6c.json
```

### 4. Recipient Verifies
```
vaultify> verify-cert
Certificate path: cert-9f8e7d6c.json
Issuer public key: keys/alice_public.pem

✓ Certificate is valid
  Token: 9f8e7d6c5b4a3210fedcba9876543210
  Expires: 2025-11-23 15:30:00
```

### 5. Manage Tokens
```
vaultify> list-tokens

Token: 9f8e7d6c5b4a...
  Credential ID: 12
  Status: ✓ Valid

vaultify> revoke-token
Token: 9f8e7d6c5b4a...
✓ Token revoked
```

---

## 🐛 Troubleshooting

### Database Connection Fails
```bash
# Recreate database volume
docker compose down -v
docker compose up -d

# Test connection
vaultify> test-db
```

### Missing Tables
```bash
# Database initialized with old schema
docker compose down -v
docker compose up
```

### Stdin Issues (Gradle)
```bash
# Use gradle runLocal instead of run
./gradlew runLocal
```

### Token Not Found
- Check `list-tokens` for correct token string
- Verify token hasn't expired
- Check if token was revoked

---

## 📊 Monitoring

### Check Ledger Integrity
```
vaultify> verify-ledger
✓ Ledger verified - 47 blocks
```

### View Database Status
```
vaultify> test-db
✓ Connection successful
  Tables: users, credentials, tokens
```

### Token Cleanup (Automatic)
- Runs every hour
- Deletes expired tokens
- Logs cleanup count

---

## 🔧 Configuration

### File: `resources/config.properties`

```properties
# Database
db.url=jdbc:postgresql://localhost:5432/vaultify
db.user=postgres
db.password=admin

# Storage
storage.mode=dual  # file, jdbc, or dual
vault.storage=./vault_data/credentials/
vault.maxFileSize=10485760  # 10MB

# Security
vault.blacklist.extensions=.mp4,.avi,.mov,.mkv,.gif

# Tokens
token.expiryHours=48
certificate.output=./vault_data/certificates/
```

---

## 📝 Notes

- **Dual Storage:** Files + PostgreSQL for redundancy
- **Graceful Shutdown:** Ctrl+C triggers cleanup
- **Token Expiry:** Cleaned automatically every hour
- **Ledger:** Immutable audit trail of all operations
- **Encryption:** Keys never stored in plaintext

---

## 🎯 Key Features

✅ **End-to-End Encryption** - All credentials encrypted at rest  
✅ **Token-Based Sharing** - Secure, revocable credential sharing  
✅ **Certificate Verification** - RSA-signed, verifiable certificates  
✅ **Blockchain Ledger** - Tamper-proof audit trail  
✅ **Dual Persistence** - File + Database redundancy  
✅ **Auto-Cleanup** - Expired tokens automatically removed  
✅ **User Isolation** - Each user has isolated vault  

---

**For detailed implementation info, see:** `IMPLEMENTATION_SUMMARY.md`  
**For architecture analysis, see:** `INCONSISTENCIES_ANALYSIS.md`
