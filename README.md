# Vaultify — Secure, Multithreaded Credential Vault (Java)

Vaultify is a local-first, encrypted credential vault implemented in pure Java. It demonstrates strong OOP design, multithreading, hybrid cryptography, JDBC persistence (PostgreSQL), and a blockchain-inspired immutable ledger for auditability. The primary user interface is a command-line dashboard.

This README summarizes the architecture, how to build and run the system (locally and with Docker), available CLI commands, common troubleshooting steps, and pointers to the important code modules.

---

Quick links
- Project root: the repository contains `src/`, `resources/`, `docker/`, and `docker-compose.yml`.
- Entry point: `com.vaultify.app.VaultifyApplication`

---

Table of contents
- What Vaultify is
- Architecture overview (packages & responsibilities)
- System layer breakdown (CLI, Model, Crypto, DAO, Service, Ledger, Threading, Util)
- Typical end-to-end flow (Add credential)
- Build & run (local Gradle and Docker Compose)
- Running the interactive CLI in Docker
- Troubleshooting & verification
- Testing notes
- Minimal code map

---

What Vaultify is
----------------
Vaultify is a secure, multithreaded, blockchain-inspired credential vault for local use. Key capabilities:
- Secure credential storage using a hybrid AES + RSA model
- Asynchronous background processing for expensive tasks (encryption, ledger writes, logging)
- Immutable audit history: a hash-linked ledger of vault operations
- CLI-first experience for demonstrations and real-world usage
- JDBC persistence using PostgreSQL and a DAO layer
- Clean OOP architecture showcasing abstraction and modularity

---

Architecture overview
---------------------
Top-level package: `com.vaultify`
- `cli/` — CLI UI & command handling (primary interface)
- `controller/` — optional REST API controllers (if present)
- `service/` — business logic layer (VaultService, UserService, LedgerService, VerificationService, TokenService)
- `model/` — domain entities (User, Vault, Credential, Token, CredentialMetadata, etc.)
- `crypto/` — cryptographic engines and utilities (AES, RSA, Hashing, KeyManager)
- `dao/` — JDBC DAO layer (UserDAO, CredentialDAO, TokenDAO, LedgerDAO)
- `ledger/` — ledger blocks & ledger engine (immutable hash-linked records)
- `threading/` or `thread/` — concurrency manager and task classes
- `util/` — file I/O, token utilities, QR generation, JSON helpers
- `config/` — centralized configuration (properties)

Design highlights
- Crypto abstraction (`CryptoEngine`) with AES & RSA implementations
- DAO pattern isolating JDBC code & transactions
- ThreadManager / ExecutorServices centralize asynchronous execution
- Ledger blocks use SHA-256 and cryptographic linking (prevHash/currentHash)

---

System layer breakdown
----------------------
CLI layer (primary)
- Text-based interface using Java input (Scanner/Console)
- Main commands:
  - `register` — create a new user + RSA keypair
  - `login` — authenticate and open vault
  - `add-credential` — add/encrypt a file/credential to vault
  - `list` — list stored credentials (metadata)
  - `share` — create a share token for a credential
  - `verify` — verify a credential or token
  - `verify-ledger` — verify ledger integrity
  - `exit` — close the CLI
- Non-blocking feedback provided while heavy tasks run in background (e.g., progress dots)

Model layer (core OOP entities)
- User — vault owner, holds RSA key identifiers
- Vault — container for credentials (composition)
- Credential — encrypted file plus metadata
- ShareToken — short-lived token for sharing access
- LedgerBlock — immutable record in ledger chain

Crypto layer
- `CryptoEngine` interface (abstraction & polymorphism)
- `RSAEngine` — RSA key ops, key wrapping, signing
- `AESEngine` — fast symmetric encryption for file payloads
- Hybrid flow: file encrypted with AES; AES key encrypted with RSA public key

DAO layer (JDBC persistence)
- DAOs use `PreparedStatement`, transactions, and a `ConnectionManager`
- Tables: `users`, `credentials`, `tokens`, `ledger`
- PostgreSQL used as the backing store

Service layer
- `VaultService` — high-level vault ops: add, retrieve, decrypt, delete credentials
- `UserService` — register/login, key management
- `LedgerService` — append & verify ledger blocks
- `VerificationService` — validate shared tokens and signatures
- All services orchestrate DAOs, crypto layers, and threading

Ledger layer (blockchain-inspired)
- `LedgerBlock` fields: `prevHash`, `currentHash`, `action`, `credentialId`, `timestamp`
- New operations append a block; integrity can be verified by recomputing hashes
- Tamper detection via `LedgerService.verifyIntegrity()`

Threading layer (concurrency backbone)
- Async tasks: `EncryptionTask` (Callable), `LedgerWriter` (Runnable), `ActivityLogger` (daemon), `TokenExpiryScheduler` (ScheduledExecutorService)
- `ThreadManager` centralizes ExecutorServices and provides safe shutdown

Utility layer
- `FileStorageUtil` — encrypted file I/O
- `QRCodeUtil` — generate QR codes for tokens
- `LoggerUtil` — structured logging helper
- `JSONUtil` — JSON serialization helpers for ledger and metadata

---

Typical end-to-end flow: Add credential
1. User issues `add-credential` in the CLI
2. CLI calls `VaultService.addCredential()`
3. `ThreadManager` submits `EncryptionTask` to an ExecutorService
4. `AESEngine` encrypts the file payload
5. `RSAEngine` encrypts the AES key with the user's public key
6. Encrypted binary written to disk by `FileStorageUtil`
7. `CredentialDAO.insert(...)` stores metadata in PostgreSQL
8. `ThreadManager` submits `LedgerWriter` which appends a new `LedgerBlock` to `ledger.json` and persists to DB
9. `ActivityLogger` records the operation async
10. CLI prints confirmation: "Encrypted and Saved"

---

Build & run
-----------
Prerequisites
- JDK 21 (for local build) or use the provided Gradle wrapper
- Docker Desktop (for Docker-based runs) + Docker Compose v2
- On Windows use `cmd.exe` or PowerShell; examples below use `cmd.exe` style where relevant

Local (Gradle) build and run

Windows (cmd.exe):

```cmd
gradlew.bat clean build
gradlew.bat runLocal
```

macOS / Linux (bash):

```bash
./gradlew clean build
./gradlew runLocal
```

Notes:
- `runLocal` is not suggested for development as it does not start a database; it assumes a local Postgres instance is running and accessible.
- `runLocal` is a convenience task that runs `com.vaultify.app.VaultifyApplication` on the classpath.
- You can also run the jar directly (after `build`):

Windows:

```cmd
java -jar build\libs\*.jar
```

Unix:

```bash
java -jar build/libs/*.jar
```

Docker (recommended for isolated and development runs)

Build images and start services (project root):

```cmd
# build images
docker compose build

# start db + app (foreground)
docker compose up

# build and start in one command
docker compose up --build
```

Background / attach mode:

```cmd
# start in background
docker compose up -d --build

# attach to interactive CLI container
docker attach vaultify_app
# detach without stopping: Ctrl+P, Ctrl+Q
```

Run a fresh interactive container for the CLI (recommended):

```cmd
docker compose run --rm app
```

Run a shell inside the app container for debugging:

```cmd
docker compose run --rm app /bin/sh
# then inside container: java -jar /app/app.jar
```

Volume mounts & persistence
- `docker-compose.yml` mounts `./vault_data` into `/app/vault_data` inside the app container. Put keys, certificates, and the ledger there for persistence and inspection.
- Postgres data persisted to a named volume `vaultify_pgdata`.

---

CLI: Common commands
- `register` — create user and RSA keypair
- `login` — log in to a vault (select user)
- `add-credential` — add and encrypt a file to the vault
- `list` — show credentials (metadata only)
- `share` — create a short-lived ShareToken
- `verify` — validate credential or token
- `verify-ledger` — verify the integrity of the entire ledger
- `exit` — quit the CLI

---

Troubleshooting & tips
----------------------
1) Docker Desktop "pipe" or connection errors (Windows)
- Symptom: errors referencing `dockerDesktopLinuxEngine` or a missing pipe.
- Remediation: start Docker Desktop, ensure the Docker daemon is running, and check `docker context ls` and `docker info`.

2) "Could not find or load main class com.vaultify.app.VaultifyApplication"
- Symptom: container (or local run) crashes with ClassNotFoundException for the main class.
- Checks:
  - Ensure `gradlew clean build` completed successfully and a jar was created in `build/libs`.
  - Inspect jar contents:

Windows:

```cmd
jar tf build\libs\*.jar | findstr VaultifyApplication
```

Unix:

```bash
jar tf build/libs/*.jar | grep VaultifyApplication
```

  - If the class is missing, ensure compilation succeeded and that `src/` is arranged correctly under package paths (`src/com/vaultify/...`).
  - The project's `jar` task is configured to include runtime classpath by unpacking dependencies; if you still see missing classes, build locally and run `java -cp build\libs\* com.vaultify.app.VaultifyApplication` for debugging.

3) Postgres connection issues
- Check `docker-compose.yml` credentials (default in project: `vaultify_db` / `vaultify_user` / `secret123`) and `resources/config.properties` if present.
- Ensure the database container is healthy and listening on port 5432.
- If running locally, ensure a Postgres instance is running and accessible with the correct credentials.

4) CLI interactivity inside Docker
- The `app` service sets `stdin_open: true` and `tty: true`. Use `docker compose run --rm app` or `docker attach vaultify_app` to get an interactive prompt.

---

Testing
-------
- Unit tests: JUnit 5 tests (service and DAO tests) should be available in the `test` source set.
- Ledger integrity test should exercise tamper detection.
- Manual CLI tests: add/remove credentials, share/verify, and verify-ledger.

Run tests locally:

```cmd
# Windows
gradlew.bat test

# Unix
./gradlew test
```

---

Minimal code map
----------------
- `src/com/vaultify/app/VaultifyApplication.java` — application entry point (CLI bootstrap)
- `src/com/vaultify/cli/CommandRouter.java` — parses commands and prints CLI dashboard
- `src/com/vaultify/crypto/` — `AESUtil`, `RSAUtil`, `HashUtil`, `KeyManager`
- `src/com/vaultify/dao/` — `UserDAO`, `CredentialDAO`, `TokenDAO`
- `src/com/vaultify/ledger/` — `LedgerEngine`, `LedgerBlock`
- `src/com/vaultify/service/` — `VaultService`, `UserService`, `LedgerService`, `VerificationService`, `TokenService`
- `resources/` — `config.properties`, DB init scripts
- `docker/` — `Dockerfile` (multi-stage Java build), `Dockerfile.postgres`
- `docker-compose.yml` — wires `db` and `app` and mounts `vault_data`

---

Security & cryptography
-----------------------
- RSA-2048 per-user keypairs
- AES-256 for content encryption
- AES keys wrapped with RSA public key
- SHA-256 ledger hashing and optional RSA signatures for blocks
- Follow secure key handling: keep private keys protected (filesystem permissions) and back up `vault_data` as needed

---