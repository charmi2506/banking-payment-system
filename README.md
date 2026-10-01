# Banking & Payment Transaction System

A secure, transaction-oriented banking backend built with **Java 21 and Spring Boot**, providing REST APIs for user authentication, account management, deposits, withdrawals, fund transfers, and transaction history.

The system focuses on backend engineering concerns such as **transaction management, concurrency control, idempotency, deadlock prevention, event-driven processing, API security, database consistency, and containerized development**.

---

## 🚀 Features

### 🔐 Authentication & Authorization
- User registration and login
- JWT-based authentication
- Spring Security integration
- Role-based authorization
- Protected REST APIs
- Secure user/account ownership validation

### 🏦 Account Management
- Create savings, checking, and business accounts
- Retrieve authenticated user's accounts
- Account balance management
- Deposit funds
- Withdraw funds
- Sufficient-balance validation
- Unique account number generation

### 💸 Fund Transfers
- Account-to-account fund transfers
- Atomic transaction processing
- PostgreSQL-backed transaction management
- Idempotency support using `X-Idempotency-Key`
- Duplicate request detection
- Cached response replay for completed idempotent requests
- Protection against concurrent duplicate transfers

### 🔒 Concurrency & Data Consistency
- `BigDecimal` for monetary calculations
- JPA pessimistic write locking
- Deterministic account-lock ordering
- Deadlock prevention
- Transaction rollback on failures
- Database-level consistency for financial operations

### 📊 Transaction History
- Paginated transaction history
- Newest-first transaction ordering
- Filter by:
  - Transaction type
  - Transaction status
  - Account
  - Date range
- Account ownership validation
- Statement endpoint
- CSV export through the web portal

### 📨 Event-Driven Processing
- Apache Kafka integration
- Transaction event publishing
- Kafka consumer for transaction events
- Audit logging
- Kafka consumer group configuration

### 📚 API Documentation
- OpenAPI 3 documentation
- Swagger UI
- JWT Bearer authentication support in Swagger
- Documented authentication, account, and transaction APIs

### 🌐 Web Banking Portal
- Responsive banking dashboard
- JWT authentication
- Account overview
- Balance information
- Deposits and withdrawals
- Fund transfers
- Transaction history
- Filtering and pagination
- CSV statement export
- Automatic idempotency-key generation for transfers

### 🐳 Containerized Infrastructure
Docker Compose is used to run:

- PostgreSQL
- Redis
- Apache Kafka

---

## 🛠️ Tech Stack

| Category | Technologies |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Messaging | Apache Kafka |
| Cache/Infrastructure | Redis |
| API Documentation | Swagger / OpenAPI |
| Build Tool | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Containerization | Docker, Docker Compose |
| Frontend | HTML, CSS, JavaScript |
| Server | Embedded Apache Tomcat |

---

## 🏗️ Architecture

The application follows a layered Spring Boot architecture.

```text
                         ┌──────────────────────┐
                         │   Web Banking Portal │
                         │   HTML/CSS/JS        │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     REST API Layer   │
                         │    Controllers       │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Security Layer    │
                         │ JWT Authentication   │
                         │ Authorization        │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Service Layer     │
                         │ Business Logic       │
                         │ Transactions         │
                         │ Idempotency          │
                         │ Concurrency Control  │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼────────────────┐
                    ▼               ▼                ▼
             ┌────────────┐ ┌────────────┐   ┌─────────────┐
             │ PostgreSQL │ │   Kafka    │   │    Redis    │
             │    JPA     │ │  Events    │   │ Infrastructure│
             └────────────┘ └─────┬──────┘   └─────────────┘
                                  │
                                  ▼
                         ┌──────────────────────┐
                         │ Kafka Event Consumer │
                         │   Audit Logging      │
                         └──────────────────────┘