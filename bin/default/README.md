# Vaultify — Secure Credential Vault System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.10-blue.svg)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

Vaultify is a **Java-based secure credential vault** system demonstrating enterprise-grade software architecture with OOP design patterns, multithreading, cryptographic abstractions, repository-driven dual persistence (PostgreSQL + filesystem), and a blockchain-inspired audit ledger.

## 🎯 Project Status

**Implementation:** Actively Refactored & Functional ✅

- ✅ **Service Layer:** Unified services with remote ledger integration
- ✅ **Crypto Layer:** AES-256-GCM + RSA-2048 OAEP implementations
- ✅ **Threading Layer:** Async logging, encryption tasks, token cleanup
- ✅ **Ledger System:** Remote Node.js ledger server (append-only blockchain)
- ✅ **Certificate System:** Token issuance & verification pipeline
- ✅ **Repository Layer:** Dual storage (PostgreSQL + filesystem) replacing legacy DAOs
- ✅ **CLI:** Interactive command-line interface for credential management
- ✅ **Build System:** Gradle 8.10 with custom tasks and fat JAR packaging
- ✅ **Docker:** Multi-container setup (PostgreSQL + App)

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Development](#-development)
- [Docker Deployment](#-docker-deployment)
- [Implementation Roadmap](#-implementation-roadmap)
- [Security Considerations](#-security-considerations)
- [Contributing](#-contributing)

## ✨ Features

### Core Architecture Features

- 🔐 **Hybrid Encryption:** AES-256-GCM for data + RSA-2048+ for key wrapping
- 🔗 **Immutable Ledger:** SHA-256 hash-linked blockchain for audit trails
- ⚡ **Multithreading:** Async encryption tasks, scheduled token cleanup, background logging
- 🗄️ **Dual Persistence:** PostgreSQL + filesystem via repository strategy
- 🎨 **Clean Architecture:** Service → Repository → Storage/DB with clear separation
- 🔌 **Pluggable Crypto:** `CryptoEngine` interface for algorithm flexibility
- 🐳 **Containerized:** Docker Compose with health checks and volume persistence

### Currently Implemented

- ✅ Full project skeleton with proper OOP structure
- ✅ Working ledger system with genesis block and integrity checks
- ✅ ThreadManager with ExecutorService pools and graceful shutdown
- ✅ Configuration management via properties files
- ✅ Docker multi-stage builds with optimized images
- ✅ Health checks for database availability
- ✅ Gradle custom tasks (runLocal, rebuild, dockerBuild)

## 🏗️ Architecture

### Design Patterns & Principles

- **Layered Architecture:** CLI → Service → Repository → Persistence (DB + Files)
- **Interface Segregation:** `CryptoEngine` abstraction for encryption algorithms
- **Dependency Injection (manual):** Constructor-based wiring (future DI container planned)
- **Factory Pattern:** RepositoryFactory selects storage mode (jdbc | file | dual)
- **Strategy Pattern:** Pluggable crypto engines (AES, RSA)
- **Template Method:** Abstract JDBC/File repository base classes
- **Singleton Pattern:** ThreadManager, configuration management

### Package Structure

```
com.vaultify/
├── app/              # Application entry point
│   └── VaultifyApplication.java
├── cli/              # Command-line interface
│   └── CommandRouter.java
├── service/          # Business logic layer
│   ├── UserService.java
│   ├── VaultService.java
│   ├── AuthService.java
│   ├── TokenService.java
│   ├── LedgerService.java
│   └── VerificationService.java
├── crypto/           # Cryptography layer
│   ├── CryptoEngine.java (interface)
│   ├── AESEngine.java
│   ├── RSAEngine.java
│   ├── KeyManager.java
│   └── HashUtil.java
├── repository/       # Repository abstractions (Postgres/File/Dual)
│   ├── UserRepository.*
│   ├── CredentialRepository.*
│   └── TokenRepository.*
├── models/           # Domain entities
│   ├── User.java
│   ├── Credential.java
│   ├── CredentialMetadata.java
│   ├── CredentialType.java
│   ├── Token.java
│   └── LedgerBlock.java (✅ Remote ledger model)
├── client/           # Remote server integration
│   └── LedgerClient.java (✅ HTTP client for ledger-server)
├── threading/        # Concurrency management
│   ├── ThreadManager.java (✅ Implemented)
│   ├── EncryptionTask.java
│   ├── ActivityLogger.java
│   └── TokenExpiryScheduler.java
├── db/               # Database connection
│   └── Database.java
├── util/             # Utilities
│   ├── Config.java
│   ├── TokenUtil.java
│   └── CredentialFileManager.java
└── verifier/         # Certificate verification
    ├── CertificateVerifier.java
    ├── CertificateParser.java
    ├── CertificateData.java
    └── VerifierMode.java
```

### Layer Responsibilities

**Service Layer** (Business Logic)

- Orchestrates workflow between DAO, crypto, and ledger layers
- Implements business rules and validation
- Manages transactions and error handling
- Current state: Method signatures defined, implementations pending

**Repository Layer** (Persistence Abstraction)

- Replaces legacy DAO classes (now removed)
- Uniform API for user, credential, token domains
- Dual mode: persists to PostgreSQL and filesystem when `storage.mode=dual`
- Encapsulates encryption metadata (JSON in DB column) for credentials

**Crypto Layer** (Security)

- Implements `CryptoEngine` interface for polymorphism
- AES-256 symmetric encryption for data
- RSA asymmetric encryption for key wrapping
- SHA-256 hashing for integrity
- Current state: Interface and method signatures ready

**Ledger Layer** (Remote Audit Trail)

- ✅ External Node.js ledger-server (Express.js)
- ✅ HTTP client integration (LedgerClient.java)
- ✅ Certificate registry and token revocation
- ✅ Public key distribution
- ✅ Cross-machine verification support
- ✅ Tamper-evident blockchain

## 📁 Project Structure

```
Vaultify/
├── src/com/vaultify/          # Source code (36 Java classes)
├── resources/
│   ├── config.properties       # Application configuration
│   └── db-scripts/
│       └── init.sql           # Database initialization
├── docker/
│   ├── Dockerfile             # Multi-stage app build
│   ├── Dockerfile.postgres    # PostgreSQL with init script
│   └── init-db.sql            # Database schema
├── ledger-server/             # Node.js ledger server
│   ├── src/                   # Server source code
│   ├── data/                  # Persistent ledger storage
│   └── package.json           # Node dependencies
├── vault_data/                # Local runtime data
│   ├── credentials/           # Encrypted credential storage
│   ├── keys/                  # RSA keypairs
│   └── certificates/          # Verification certificates
├── build.gradle               # Build configuration
├── docker-compose.yml         # Container orchestration
├── .env.example              # Environment template
└── README.md                 # This file
```

## 📦 Prerequisites

### For Local Development

- **JDK 21** (Eclipse Temurin recommended)
- **Gradle 8.10+** (included via wrapper)
- **PostgreSQL 16** (if running locally without Docker)

### For Docker Deployment

- **Docker Engine 24+**
- **Docker Compose v2.20+**

### Verify Installation

```powershell
# Check Java
java -version

# Check Gradle
.\gradlew.bat --version

# Check Docker
docker --version
docker compose version
```

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```powershell
# Clone the repository
git clone https://github.com/HetMistri/Vaultify.git
cd Vaultify

# Copy environment template (optional)
cp .env.example .env

# Build and run with Docker Compose
docker compose up --build

# Or run interactively for CLI
docker compose run --rm app
```

### Option 2: Local Build

```powershell
# Build the project
.\gradlew.bat clean build

# Run custom tasks
.\gradlew.bat rebuild      # Clean + build
.\gradlew.bat runLocal     # Run without Docker (needs local PostgreSQL)
.\gradlew.bat dockerBuild  # Build Docker images

# Run the JAR directly
java -jar build\libs\Vaultify-1.0.0.jar
```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file from the template:

```bash
# Database Configuration
POSTGRES_DB=vaultify_db
POSTGRES_USER=vaultify_user
POSTGRES_PASSWORD=your_secure_password_here
```

### Configuration File (`resources/config.properties`)

```properties
# Database Connection
db.url=jdbc:postgresql://db:5432/vaultify_db
db.user=vaultify_user
db.password=secret123

# Storage Paths
vault.storage=./vault_data/credentials/
ledger.file=./vault_data/ledger.json
rsa.public=./vault_data/keys/public.pem
rsa.private=./vault_data/keys/private.pem

# Token Settings
token.expiryHours=48
certificate.output=./vault_data/certificates/
```

**⚠️ Security Notes:**

- Never commit `.env` with real credentials
- Use strong passwords in production
- Rotate credentials regularly
- Consider using Docker secrets for production deployments

## 🛠️ Development

### Build Tasks

```powershell
# Standard Gradle tasks
.\gradlew.bat build          # Compile and package
.\gradlew.bat test           # Run tests
.\gradlew.bat clean          # Clean build artifacts

# Custom Vaultify tasks
.\gradlew.bat runLocal       # Run locally (requires PostgreSQL)
.\gradlew.bat rebuild        # Clean + build
.\gradlew.bat dockerBuild    # Build Docker images

# View all tasks
.\gradlew.bat tasks --group=vaultify
```

### Project Statistics

- **Total Classes:** 36 Java files
- **Packages:** 11 distinct packages
- **Lines of Code:** ~1,500+ (skeleton implementation)
- **Dependencies:** 2 (PostgreSQL JDBC, Gson)
- **Build Tool:** Gradle 8.10
- **Target Platform:** Java 21 (LTS)

### Key Implementation Details

**Ledger System (Fully Implemented)**

```java
// Genesis block creation
LedgerEngine engine = new LedgerEngine();

// Add blocks
LedgerBlock block = engine.addBlock("ADD_CREDENTIAL", dataHash);

// Verify integrity
List<String> errors = engine.verifyIntegrity();
```

**Threading (Fully Implemented)**

```java
// Async execution
ThreadManager.runAsync(() -> {
    // Background task
});

// Scheduled tasks
ThreadManager.scheduleAtFixedRate(task, 0, 1, TimeUnit.HOURS);

// Graceful shutdown
ThreadManager.shutdown();
```

**Crypto Interface (Ready for Implementation)**

```java
CryptoEngine aes = new AESEngine();
byte[] encrypted = aes.encrypt(plaintext);
byte[] decrypted = aes.decrypt(encrypted);
```

## 🐳 Docker Deployment

### Docker Architecture

**Multi-Stage Build:**

- **Stage 1:** Uses `gradle:8.6-jdk21` to build the application
- **Stage 2:** Uses `eclipse-temurin:21-jre` for minimal runtime image

**Services:**

- `db` - PostgreSQL 16 with health checks
- `app` - Vaultify application with interactive TTY

### Docker Commands

```powershell
# Build and start all services
docker compose up --build

# Run in background
docker compose up -d --build

# View logs
docker compose logs -f app

# Stop services
docker compose down

# Remove volumes (clean slate)
docker compose down -v

# Interactive CLI session
docker compose run --rm app

# Debug shell
docker compose run --rm app /bin/sh
```

### Docker Features

✅ **Health Checks:** Database waits for PostgreSQL readiness  
✅ **Volume Persistence:** Data survives container restarts  
✅ **Environment Variables:** Externalized configuration  
✅ **Build Caching:** Faster subsequent builds  
✅ **Interactive TTY:** Full CLI support in containers

### Troubleshooting Docker

**Database Connection Issues:**

```powershell
# Check database health
docker compose ps
docker compose logs db

# Verify network connectivity
docker compose exec app ping db
```

**Container Won't Start:**

```powershell
# Check for port conflicts
netstat -ano | findstr :5432

# View container logs
docker compose logs app
```

## 🗺️ Implementation Roadmap

### ✅ Phase 1: Architecture & Skeleton (Complete)

- [x] Package structure and class organization
- [x] Service layer interfaces
- [x] CryptoEngine abstraction with AES/RSA implementations
- [x] ThreadManager with executor pools
- [x] LedgerEngine with blockchain functionality
- [x] Gradle build configuration with custom tasks
- [x] Docker multi-stage builds
- [x] Docker Compose with health checks

### 🔄 Phase 2: Core Implementation (In Progress)

- [ ] Implement AES-256-GCM encryption/decryption
- [ ] Implement RSA-2048+ key generation and operations
- [ ] KeyManager for PEM key persistence
- [ ] Database connection pooling
- [ ] DAO layer with PreparedStatements
- [ ] Service layer business logic
- [ ] CLI command routing and handlers

### 📅 Phase 3: Advanced Features (Planned)

- [ ] User registration and authentication
- [ ] Credential encryption and storage
- [ ] Token generation and validation
- [ ] Certificate verification
- [ ] Ledger integrity monitoring
- [ ] Activity logging and audit trails

### 🧪 Phase 4: Testing & Documentation (Planned)

- [ ] Unit tests for crypto operations
- [ ] Integration tests for services
- [ ] End-to-end CLI tests
- [ ] Performance benchmarks
- [ ] Security audit
- [ ] API documentation

## 🔐 Security Considerations

### Current Security Posture

**✅ Implemented:**

- SHA-256 hashing for ledger integrity
- Immutable audit trail via blockchain design
- SecureRandom for key/IV generation
- Environment variable support for credentials
- `.env` excluded from version control

**⚠️ Pending Implementation:**

- AES-GCM authenticated encryption
- RSA-OAEP padding for key wrapping
- Key derivation functions (PBKDF2/Argon2)
- TLS for database connections
- Input validation and sanitization
- SQL injection prevention via PreparedStatements
- Rate limiting for authentication attempts

### Best Practices (When Implementing)

1. **Never hardcode secrets** - Use environment variables or secrets management
2. **Use strong algorithms** - AES-256-GCM, RSA-2048+, SHA-256
3. **Proper key storage** - Encrypt private keys at rest, use OS keychains
4. **Secure random** - Use `SecureRandom` for all cryptographic randomness
5. **Input validation** - Validate and sanitize all user inputs
6. **Least privilege** - Database users with minimal required permissions
7. **Audit logging** - Log all security-relevant events to ledger

## 🧪 Testing

### Test Strategy

**Current Status:** Test infrastructure ready, implementation pending

```powershell
# Run all tests
.\gradlew.bat test

# Run with coverage
.\gradlew.bat test jacocoTestReport

# Run specific test class
.\gradlew.bat test --tests "LedgerEngineTest"
```

### Planned Test Coverage

- **Unit Tests:** Crypto operations, hashing, key generation
- **Integration Tests:** Service layer with database interactions
- **End-to-End Tests:** Full workflow simulation via CLI
- **Performance Tests:** Encryption throughput, ledger performance
- **Security Tests:** SQL injection, input validation, crypto strength

## 🤝 Contributing

### Development Workflow

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Coding Standards

- **Java Style:** Follow Oracle Java conventions
- **Naming:** Clear, descriptive variable/method names
- **Documentation:** Javadoc for all public APIs
- **Testing:** Unit tests for all new features
- **Security:** No hardcoded secrets, validate inputs

### Branch Strategy

- `master` - Production-ready code
- `dev` - Integration branch
- `feature/*` - Feature development branches
- `bugfix/*` - Bug fix branches

## 📊 Project Metrics

| Metric              | Value                       |
| ------------------- | --------------------------- |
| Java Classes        | ~34 (post-DAO removal)      |
| Packages            | 10                          |
| Services            | 6                           |
| Repository Families | 3 (User, Credential, Token) |
| Crypto Engines      | 2 (AES, RSA)                |
| Threaded Tasks      | 4                           |
| Model Classes       | 5+                          |
| Gradle Version      | 8.10                        |
| Java Version        | 21 (LTS)                    |
| Docker Images       | 2 (app, db)                 |

### Persistence Refactor Summary

Legacy DAO classes (`*DAO.java`) have been fully removed and replaced with a repository pattern:

- `RepositoryFactory` selects implementation based on `storage.mode` (jdbc | file | dual)
- Dual mode writes both to PostgreSQL (structured metadata) and filesystem (encrypted blobs)
- Credential encryption metadata (`encryptedKeyBase64`, `ivBase64`, `dataHash`, `fileSize`) serialized as JSON inside the DB credential row for portability
- Eliminates duplicated logic and reduces surface area for persistence-related bugs
- Stubs were deprecated then deleted after verification of zero external references

For detailed rationale and migration notes see `docs/ARCHITECTURE_PERSISTENCE.md`.

## 📝 Code Map

### Entry Points

- `VaultifyApplication.java` - Main application entry
- `CommandRouter.java` - CLI command dispatcher

### Core Components

- `LedgerEngine.java` ✅ - Blockchain audit trail (fully implemented)
- `ThreadManager.java` ✅ - Async execution manager (fully implemented)
- `HashUtil.java` ✅ - SHA-256 hashing (fully implemented)

### Service Layer (Skeleton)

- `UserService` - User registration and authentication
- `VaultService` - Credential management
- `AuthService` - Login/logout operations
- `TokenService` - Token generation and validation
- `LedgerService` - Ledger operations wrapper
- `VerificationService` - Certificate and token verification

### Crypto Layer (Skeleton)

- `CryptoEngine` (interface) - Encryption abstraction
- `AESEngine` - Symmetric encryption implementation
- `RSAEngine` - Asymmetric encryption implementation
- `KeyManager` - Key storage and retrieval

## 📄 License

This project is developed as an academic assignment for demonstrating software engineering principles.

## 👥 Authors

- **Het Mistri** - Architecture, Services, Threading, Crypto
- **Team** - Collaborative development

## 🔗 Links

- **GitHub Repository:** [HetMistri/Vaultify](https://github.com/HetMistri/Vaultify)
- **Issue Tracker:** [GitHub Issues](https://github.com/HetMistri/Vaultify/issues)

---

**Last Updated:** November 14, 2025  
**Version:** 1.0.0 (Day 1 - Architecture Complete)  
**Status:** 🟡 In Development - Architecture Phase Complete

- RSA-2048 per-user keypairs
- AES-256 for content encryption
- AES keys wrapped with RSA public key
- SHA-256 ledger hashing and optional RSA signatures for blocks
- Follow secure key handling: keep private keys protected (filesystem permissions) and back up `vault_data` as needed

---
