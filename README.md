# Banking & Payment Transaction System

A production-style, high-throughput Banking & Payment Transaction System built with **Java 21** and **Spring Boot 3**.

## Features & Highlights

- **Transactional Consistency**: Strict `@Transactional(rollbackFor = Exception.class)` boundaries ensuring ACID guarantees across monetary operations.
- **Precision Financial Math**: Exclusive use of `BigDecimal` for monetary amounts to eliminate floating-point rounding errors.
- **Concurrent Transaction Control**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on account balances to prevent race conditions, lost updates, and double-spending.
- **Idempotent Transaction Processing**: PostgreSQL-backed `IdempotencyRecord` tracking to prevent duplicate transactions on retries using an idempotency key.
- **Event-Driven Architecture**: Kafka topics for transaction events, audit logging, and customer notifications.
- **Security & Authorization**: Spring Security with JWT token-based authentication and Role-Based Access Control (RBAC).
- **Caching & Session Control**: Redis support for caching and rate limiting.

---

## Technology Stack

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security & JJWT
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka (KRaft mode)
- **Cache**: Redis 7
- **Object Mapping**: MapStruct & Lombok
- **Testing**: JUnit 5 & Mockito

---

## Architecture Overview

```
src/main/java/com/banking/payment/
├── config/             # Security, Kafka, Redis, and JPA configuration
├── controller/         # REST API endpoints (Auth, Accounts, Transactions)
├── service/            # Core business logic & Idempotency execution
│   └── impl/           # Service implementation classes
├── repository/         # Spring Data JPA repositories with Pessimistic Locking
├── entity/             # JPA domain entities (User, Account, Transaction, IdempotencyRecord)
│   └── enums/          # Domain enums (Role, TransactionStatus, AccountType, etc.)
├── dto/                # Data Transfer Objects
│   ├── request/        # API request payloads
│   └── response/       # API response payloads
├── mapper/             # MapStruct mapper interfaces
├── exception/          # Global Exception Handler & custom exceptions
└── security/           # JWT filters, Token Provider, UserDetailsService
```

---

## Getting Started

### Prerequisites

- **Java**: 21 or higher
- **Maven**: 3.8+
- **Docker & Docker Compose**

### 1. Environment Setup

Copy `.env.example` to `.env` or set environment variables in your environment:

```bash
cp .env.example .env
```

### 2. Start Infrastructure Services

Spin up PostgreSQL, Redis, and Kafka using Docker Compose:

```bash
docker compose up -d
```

### 3. Build & Run Application

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.
