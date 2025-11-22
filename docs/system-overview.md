# VAULTIFY — COMPLETE SYSTEM OVERVIEW (ACTUAL IMPLEMENTATION REPORT)

This document reconciles the intended design (spec text & provided sequence diagram image) with the CURRENT state of the source tree on branch `feature/het-main`. It highlights what exists, what is partially implemented, and what is missing or differs from the aspirational description.

## Contents

1. High-Level Purpose
2. Layered Architecture (Actual vs Claimed)
3. Package-by-Package Inventory
4. Domain Model Summary
5. Crypto & Security Model (Actual State)
6. Persistence Strategy (Dual File + JDBC)
7. Ledger (Blockchain-Inspired Audit)
8. Concurrency & Threading State
9. Certificate & Token Flow
10. CLI Command Surface
11. Testing & Quality Status
12. Variances vs Provided Spec / Diagram
13. Recommended Next Actions

14. High-Level Purpose

---

Vaultify is a local-first credential/file vault. Files are encrypted using an AES-256-GCM data key that is RSA-OAEP wrapped per user. Each significant operation (add/delete credential, generate token) is recorded as a hash-linked ledger block enabling tamper detection. Interaction currently occurs through a CLI loop.

2. Layered Architecture (Actual vs Claimed)

---

| Layer             | Claimed (Spec) | Actual Packages                             | Notes                                                                |
| ----------------- | -------------- | ------------------------------------------- | -------------------------------------------------------------------- |
| CLI               | `cli/`         | `cli/`                                      | Implemented via `CommandRouter` interactive loop.                    |
| Controller / REST | `controller/`  | (Absent)                                    | Not present in repository.                                           |
| Service           | `service/`     | `service/`                                  | Auth, Vault, Ledger, Token, Verification, User services exist.       |
| Model             | `model/`       | `models/`                                   | Domain entities in `models/`. Naming differs (plural).               |
| Crypto            | `crypto/`      | `crypto/`                                   | AES + RSA + Hash + KeyManager implemented.                           |
| DAO (JDBC)        | `dao/`         | `dao/`                                      | File & JDBC variants coexist; some legacy DAO classes. No LedgerDAO. |
| Ledger            | `ledger/`      | `ledger/`                                   | Implemented (`LedgerEngine`, `LedgerBlock`).                         |
| Thread            | `thread/`      | `threading/`                                | Directory named `threading`. Contains skeleton tasks.                |
| Util              | `util/`        | `util/`                                     | Config, CredentialFileManager, TokenUtil, FileStorageUtil.           |
| Config            | `config/`      | (Merged into `resources/config.properties`) | No separate `config/` package; config via properties + env.          |

3. Package-by-Package Inventory

---

### com.vaultify.app

`VaultifyApplication` - Main entrypoint; launches CLI.

### com.vaultify.cli

`CommandRouter` - Parses user commands; includes a diagnostic `test-db` command; vault subcommands partially stubbed (TODO integration warnings remain in earlier lines prior edits).

### com.vaultify.service

- `AuthService` - Registration/login, dual persistence (File + JDBC). Stores encrypted private key (AES over PKCS#8) & public key (Base64). Session management.
- `VaultService` - Encryption/decryption workflow, dual metadata persistence, asynchronous ledger writes.
- `LedgerService` - Simplified wrapper for ledger append & verify.
- `TokenService` - Token generation, certificate creation & revocation list (in-memory only).
- `VerificationService` - Certificate verification via signature & ledger linkage.
- `UserService` - Facade around Auth + file user lookup; partly redundant.

### com.vaultify.dao

File-based: `FileUserDAO`, `FileCredentialDAO`, `FileTokenDAO` (JSON persistence).
JDBC-based: `JdbcUserDAO`, `JdbcCredentialDAO`, `JdbcTokenDAO` (SQL ops). Legacy classes `UserDAO`, `CredentialDAO` (transitional, not formal interfaces). No dedicated `LedgerDAO` implemented despite claim.

### com.vaultify.crypto

`CryptoEngine` interface; `AESEngine` (AES-256-GCM utilities), `RSAEngine` (RSA-OAEP SHA-256), `HashUtil` (SHA-256 hex), `KeyManager` (PEM key loading). **Hybrid model realized in CredentialFileManager.**

### com.vaultify.ledger

`LedgerEngine` - Genesis block initialization, JSON persistence, synchronized methods. `LedgerBlock` - Fields: index, timestamp, action, dataHash, prevHash, hash.

### com.vaultify.threading

`ThreadManager` - Fixed + scheduled executors, graceful shutdown. Skeletons: `EncryptionTask`, `LedgerWriter`, `ActivityLogger`, `TokenExpiryScheduler`. Logic incomplete (placeholders). No implemented `VaultLoader` despite spec.

### com.vaultify.util

`Config` (env override), `CredentialFileManager` (core hybrid encryption pipeline), `FileStorageUtil`, `TokenUtil`. No `QRCodeUtil`, `LoggerUtil`, `JSONUtil` as claimed.

### com.vaultify.verifier

`Certificate`, `CertificateParser`, `CertificateVerifier`, `CertificateData`, `VerifierMode`. Concern: separate `CertificateData` & `Certificate` may be consolidated.

### com.vaultify.models

Entities: `User`, `Credential`, `CredentialMetadata`, `CredentialType`, `Token`. Separation between DB row style (`Credential`) and encryption metadata (`CredentialMetadata`).

### com.vaultify.db

`Database` - Raw driver-based connection factory; missing pooling (HikariCP recommended).

### com.vaultify.test

`AuthTest` - Manual console runner, not integrated JUnit.

4. Domain Model Summary

---

| Entity             | Role                     | Key Fields                                                                                                   |
| ------------------ | ------------------------ | ------------------------------------------------------------------------------------------------------------ |
| User               | Account & key pair owner | id, username, passwordHash, publicKey, privateKeyEncrypted, createdAt                                        |
| CredentialMetadata | Encryption meta          | credentialIdString (UUID), dataHash, fileSize, encryptedKeyBase64, ivBase64, userId, timestamp               |
| Credential         | DB row style (JDBC)      | id, userId, filename, filepath, metadata, createdAt                                                          |
| Token              | Sharing mechanism        | id, credentialId, token, expiry                                                                              |
| LedgerBlock        | Audit record             | index, timestamp, action, dataHash, prevHash, hash                                                           |
| Certificate        | Share artifact           | token, issuerUserId, credentialId, expiryEpochMs, payloadHash, signatureBase64, ledgerBlockHash, createdAtMs |

5. Crypto & Security Model (Actual State)

---

Implemented:

- AES-256-GCM symmetric encryption (random key + IV per file).
- RSA-2048 key pair generation (configurable size parameter currently set to 2048).
- RSA-OAEP with SHA-256 for key wrapping.
- SHA-256 hashing for ledger linkage & password hashing.
- Certificate signing using `SHA256withRSA` signatures.

Gaps / Risks:

- Password hashing uses raw SHA-256 (no salt, no stretching).
- AES key derivation for private key encryption: manual substring approach; replace with PBKDF2/Argon2.
- No integrity/HMAC for JSON metadata files.
- No key rotation strategy.

6. Persistence Strategy (Dual File + JDBC)

---

Current Behavior:

- User & credential metadata written to both file system and PostgreSQL (best-effort; DB failures logged as warnings).
- Retrieval attempts DB first (performance) then file fallback.
- Encrypted binary contents stored only on disk under `vault_data/credentials/`.

Missing:

- Consistency reconciliation (drift detection between stores).
- Transaction bundling for dual writes (write-ahead & rollback strategy).
- Ledger persisted only as a single JSON file (no DB mirror).

7. Ledger (Blockchain-Inspired Audit)

---

- Genesis block auto-created if ledger empty.
- Each block includes previous hash; integrity verification recomputes chain.
- Actions recorded: `ADD_CREDENTIAL`, `DELETE_CREDENTIAL`, `GENERATE_TOKEN` (others possible).
- Persistence: Pretty-printed JSON file. **No compaction or archival policy.**

8. Concurrency & Threading State

---

- `ThreadManager`: Fixed thread pool (4) + scheduled pool (2). Hard-coded sizes.
- Asynchronous ledger writes and potential future encryption offloads.
- Skeleton classes exist but logic missing: `EncryptionTask` (should encapsulate AES + RSA wrap), `ActivityLogger` (async audit log), `LedgerWriter` (batched ledger persistence), `TokenExpiryScheduler` (expiration cleanup minimal wrapper). Missing spec items: `VaultLoader`, advanced pipeline.

9. Certificate & Token Flow

---

Flow (Implemented): TokenService: generate token → hash payload → sign with issuer private key → append ledger → persist certificate JSON → later verification matches ledger block & signature & expiry.

Limitations:

- In-memory revocation list not persisted.
- No token scope constraints beyond credentialId.
- No replay detection beyond revocation set.

10. CLI Command Surface

---

Implemented commands:

- `register`, `login`, `logout`, `whoami`
- `vault` submenu: `add`, `list`, `view`, `delete` (wiring completeness should be reviewed)
- `share` (certificate generation)
- `verify-cert` (certificate validation)
- `verify-ledger` (integrity check)
- `verify` (token placeholder)
- `test-db` (schema diagnostics)
- `help`, `exit`

Not Implemented / Claimed in Spec:

- `add-credential` as top-level (mapped inside `vault` instead).
- Progress indicators / UX refinements (spec mentions progress dots; not present).

11. Testing & Quality Status

---

Actual:

- Manual `AuthTest` only.
- No JUnit tests, no automated crypto/ledger validation suite.

Spec Claims (Not Present):

- JUnit 5 service tests, DAO tests, ledger tamper tests.

12. Variances vs Provided Spec / Diagram

---

| Spec / Diagram Claim                              | Status   | Notes                                           |
| ------------------------------------------------- | -------- | ----------------------------------------------- |
| REST `controller/` layer                          | Missing  | Future enhancement optional.                    |
| LedgerDAO                                         | Missing  | Ledger purely file-based.                       |
| LoggerUtil / QRCodeUtil / JSONUtil                | Missing  | Could be added; JSON handled via Gson directly. |
| VaultLoader threaded component                    | Missing  | No parallel bulk decrypt logic yet.             |
| TokenExpiryScheduler full logic                   | Partial  | Scheduler wrapper exists; no cleanup runnable.  |
| Advanced progress feedback                        | Missing  | CLI does synchronous prints only.               |
| JUnit 5 test suite                                | Missing  | Needs scaffolding.                              |
| Transactions & ConnectionManager abstractions     | Partial  | Direct DriverManager usage only.                |
| Strong password KDF (Argon2/PBKDF2)               | Missing  | Raw SHA-256 currently.                          |
| Structured metadata columns (hash, size separate) | Partial  | DB uses concatenated string.                    |
| Dual-store reconciliation                         | Missing  | No consistency audit.                           |
| Activity Logger                                   | Skeleton | Runnable present; logic absent.                 |
| EncryptionTask                                    | Skeleton | Callable present; logic absent.                 |

13. Recommended Next Actions

---

Priority 1 (Security):

- Replace password hashing with Argon2 or PBKDF2 (salt + iterations).
- Replace AES key derivation for private key encryption with KDF output.
- Add integrity check (HMAC) for ledger & metadata files.

Priority 2 (Reliability & Data):

- Implement composite DAO with transactional dual write semantics.
- Add reconciliation/audit task to compare DB and file metadata.
- Introduce HikariCP for JDBC connection pooling.

Priority 3 (Completeness):

- Flesh out `EncryptionTask`, `ActivityLogger`, `LedgerWriter`, token expiry cleanup.
- Implement missing utilities (or adjust spec to remove if unneeded).
- Create JUnit 5 test suite (crypto correctness, ledger tamper cases, dual-storage consistency).

Priority 4 (Observability & UX):

- Integrate SLF4J + Logback for structured logging.
- Progress indicators for long-running encrypt/decrypt operations.
- Metrics endpoint (if REST added) or CLI diagnostics (`stats` command).

Priority 5 (Extensibility):

- Optional REST layer for remote interactions.
- Pluggable key storage (external KMS, smart card integration).
- Ledger rotation / archival strategy.

## Status Summary

The system implements its core vision (hybrid encryption, ledger audit, dual persistence, CLI interface). Several aspirational features listed in the spec and depicted in the sequence diagram remain **unimplemented or skeletal** (REST/controller, advanced threading tasks, logging utilities, comprehensive automated tests, QR code generation, password KDF). Security hardening and reliability features should be prioritized for a production‑grade milestone. Current maturity: Mid-Alpha with strong architectural scaffold.

Prepared: 2025-11-22
Branch: feature/het-main
