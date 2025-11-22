# Vaultify Ledger Server

**Public Certificate Registry • Audit Ledger • Token Revocation Service**

A standalone Express.js server that provides a tamper-evident, append-only ledger for Vaultify's certificate verification system.

## 🏗 Architecture

```
┌─────────────────┐         ┌─────────────────┐
│   Vaultify A    │         │   Vaultify B    │
│  (Local-First)  │         │  (Local-First)  │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │    REST API Calls         │
         └───────────┬───────────────┘
                     │
          ┌──────────▼──────────┐
          │   Ledger Server     │
          │  ┌──────────────┐   │
          │  │ Ledger Chain │   │
          │  │ Certificates │   │
          │  │  Revocation  │   │
          │  └──────────────┘   │
          └─────────────────────┘
```

## 🔐 What Data is Stored?

**Only public, non-sensitive data:**

- ✅ Certificate metadata
- ✅ Token hashes (SHA-256)
- ✅ Public keys
- ✅ Signatures
- ✅ Ledger block hashes
- ✅ Timestamps

**Never stored:**

- ❌ Private keys
- ❌ User passwords
- ❌ Encrypted credentials
- ❌ Plaintext tokens
- ❌ Raw credential data

## 🚀 Quick Start

### Installation

```bash
cd ledger-server
npm install
```

### Run Server

```bash
# Development mode (auto-reload)
npm run dev

# Production mode
npm start
```

Server starts on `http://localhost:3000`

## 📡 API Endpoints

### Ledger Operations

#### Append Block

```http
POST /api/ledger/blocks
Content-Type: application/json

{
  "action": "GENERATE_CERT",
  "dataHash": "sha256_hash_of_data"
}
```

#### Get Block by Hash

```http
GET /api/ledger/blocks/:hash
```

#### Get All Blocks

```http
GET /api/ledger/blocks
```

#### Get Block by Index

```http
GET /api/ledger/blocks/index/:index
```

### Certificate Operations

#### Register Certificate

```http
POST /api/certificates
Content-Type: application/json

{
  "certificateId": "unique_id",
  "payload": {
    "issuerUserId": "user_id",
    "credentialId": "cred_id",
    "tokenHash": "sha256_token_hash",
    "expiry": 1735689600000,
    "ledgerBlockHash": "block_hash"
  },
  "signature": "rsa_signature",
  "issuerPublicKey": "-----BEGIN PUBLIC KEY-----..."
}
```

#### Get Certificate

```http
GET /api/certificates/:certificateId
```

#### Get All Certificates

```http
GET /api/certificates
```

### Token Revocation

#### Revoke Token

```http
POST /api/tokens/revoke
Content-Type: application/json

{
  "tokenHash": "sha256_hash",
  "reason": "User requested revocation"
}
```

#### Check if Token is Revoked

```http
GET /api/tokens/revoked/:tokenHash
```

### Public Key Lookup

#### Register Public Key

```http
POST /api/users/:userId/public-key
Content-Type: application/json

{
  "publicKey": "-----BEGIN PUBLIC KEY-----..."
}
```

#### Get User Public Key

```http
GET /api/users/:userId/public-key
```

## 🧪 Testing

```bash
npm test
```

This runs integration tests that verify:

- Block creation and chain integrity
- Certificate registration and retrieval
- Token revocation
- Full verification flow

## 📁 Data Storage

Data is stored in JSON files in the `data/` directory:

- `ledger.json` - Blockchain ledger
- `certificates.json` - Certificate registry
- `revoked-tokens.json` - Revoked token hashes
- `public-keys.json` - User public keys

**Production Note:** For production deployment, consider using PostgreSQL or MongoDB instead of JSON files.

## 🔗 Vaultify Integration

### In Vaultify (Java/CLI)

Use the `LedgerClient` class to communicate with the server:

```java
LedgerClient ledger = new LedgerClient("http://localhost:3000");

// Append block
Block block = ledger.appendBlock("GENERATE_CERT", dataHash);

// Register certificate
ledger.registerCertificate(certificate);

// Check revocation
boolean isRevoked = ledger.isTokenRevoked(tokenHash);

// Verify certificate
boolean isValid = ledger.verifyCertificate(certificateId, token);
```

## 🔒 Security Features

- **Immutable Ledger:** Blocks are cryptographically linked via SHA-256 hashing
- **Certificate Verification:** RSA signature validation ensures authenticity
- **Token Revocation:** Real-time revocation check prevents compromised tokens
- **Tamper Detection:** Any modification to previous blocks breaks the chain
- **No Sensitive Data:** Only public audit information is stored

## 🎯 Verification Flow

1. User A generates certificate → Posts to ledger
2. User A shares `token` + `certificateId` with User B
3. User B fetches certificate from ledger
4. System verifies:
   - ✅ Signature matches issuer's public key
   - ✅ Token hash matches certificate
   - ✅ Block exists and is anchored in chain
   - ✅ Token not revoked
   - ✅ Certificate not expired
5. Return **VALID** or **INVALID**

## 🌐 Deployment

### Local Development

```bash
npm run dev
```

### Production (with PM2)

```bash
npm install -g pm2
pm2 start src/server.js --name vaultify-ledger
```

### Docker (Optional)

```bash
docker build -t vaultify-ledger .
docker run -p 3000:3000 vaultify-ledger
```

## 📊 Project Structure

```
ledger-server/
├── src/
│   ├── server.js              # Express app entry point
│   ├── models/
│   │   ├── Block.js           # Ledger block model
│   │   └── Certificate.js     # Certificate model
│   ├── services/
│   │   ├── LedgerService.js   # Blockchain logic
│   │   ├── CertificateService.js
│   │   └── TokenService.js
│   ├── controllers/
│   │   ├── ledgerController.js
│   │   ├── certificateController.js
│   │   └── tokenController.js
│   ├── routes/
│   │   └── index.js           # API routes
│   └── utils/
│       ├── crypto.js          # Hashing utilities
│       └── storage.js         # JSON file operations
├── data/                      # JSON data storage
├── tests/
│   └── test-ledger.js
├── package.json
└── README.md
```

## 🤝 Contributing

This is part of the Vaultify project submission.

## 📝 License

MIT
