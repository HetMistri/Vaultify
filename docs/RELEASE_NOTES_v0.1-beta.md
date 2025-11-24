# Vaultify v0.1 Beta Release Notes

**Date:** November 24, 2025
**Version:** 0.1-beta

## 🚀 Release Overview

Vaultify v0.1 Beta is the first major milestone release of the Secure Credential Vault System. This release establishes the core architecture for secure, decentralized credential management, featuring enterprise-grade encryption, dual-layer persistence, and a blockchain-inspired audit ledger.

## ✨ Key Features

### 🔐 Security & Cryptography

- **AES-256-GCM Encryption:** All credentials are encrypted at rest using industry-standard AES-256 in GCM mode.
- **RSA-2048 Key Pairs:** Each user is assigned a unique RSA key pair upon registration. Private keys are encrypted with the user's password.
- **Secure Sharing:** Share credentials securely using time-limited tokens and signed certificates (X.509 style).

### 💾 Persistence & Storage

- **Dual-Layer Repository:**
  - **PostgreSQL:** Stores structured metadata, user accounts, and token validity.
  - **File System:** Stores encrypted credential payloads (blobs) for efficient handling of large files.
- **Drift Detection:** Built-in reconciliation tools to detect inconsistencies between the database and file storage.

### 📒 Audit & Ledger

- **Remote Ledger Server:** A separate Node.js service acting as an immutable audit log.
- **Blockchain Architecture:** Ledger entries are cryptographically linked (SHA-256), ensuring tamper evidence.
- **Verification:** CLI tools to verify the integrity of the local chain against the remote server.

### 💻 Command Line Interface (CLI)

- **Interactive Shell:** A robust shell environment for managing the vault.
- **Modular Architecture:** Recently refactored `CommandRouter` splits logic into dedicated handlers (`Auth`, `Vault`, `Token`, `System`) for better maintainability.
- **Dev Mode:** Special tooling for developers (`test-db`, `test-ledger`, `reset-all`) to facilitate rapid testing and debugging.

## 🛠️ Technical Improvements

- **Refactored CLI:** The monolithic `CommandRouter` (1500+ lines) has been split into modular handlers, improving code readability and extensibility.
- **Health Checks:** New `health` command provides instant diagnostics for DB connectivity, ledger status, and storage permissions.
- **Reconciliation:** New `reconcile` command scans for orphaned files or missing database entries.
- **Threading:** Asynchronous activity logging and background token cleanup tasks.

## 📦 Installation & Running

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Node.js (for Ledger Server)

### Quick Start

1. **Start Infrastructure:**
   ```bash
   docker compose up -d
   ```
2. **Start Ledger Server:**
   ```bash
   cd ledger-server
   npm install
   npm start
   ```
3. **Run Vaultify CLI:**
   ```bash
   ./gradlew run
   ```

## 📝 Known Issues

- This is a **Beta** release. While core functionality is stable, edge cases in network connectivity with the ledger server may require a retry.
- `reset-all` is destructive and should only be used in development environments.

---

_Vaultify Team_
