# Distributed Wallet & Ledger System

A backend payment engine built to demonstrate correctness under
concurrency, reliability under failure, and defensible engineering
trade-offs — not just CRUD.

Built in Java / Spring Boot, this project models the core of a real
digital wallet platform: users hold balances, transfer money to one
another, and every transaction is provably correct even under
concurrent load, network retries, partial failures, and a downed
message broker.

## Why this project exists

Most portfolio projects prove you can build features. This one proves
something different: that the system stays correct when things go
wrong. Every core decision below was chosen to solve a specific,
real failure mode — and each one was validated with evidence, not
just claimed.

## Core engineering decisions

**Double-entry ledger, not a mutable balance column.**
A wallet's balance is never stored directly — it's derived by summing
an append-only ledger of DEBIT/CREDIT entries. A balance can never
silently drift from its own transaction history, because there's
nothing to drift; the ledger *is* the balance.

**Idempotent transfer API.**
Every transfer requires a client-supplied `Idempotency-Key`. A retried
request (e.g. after a client-side timeout) returns the original result
instead of executing a second time — verified by sending the same
request twice and confirming only one balance change occurred.

**Concurrency safety, proven with a failing test first.**
A dedicated test fires 10 simultaneous transfer requests against a
wallet that can only afford 5. Before adding row-level locking, this
test reliably drove the wallet to a **-500 negative balance** — a real,
reproducible race condition. After adding `SELECT ... FOR UPDATE`
locking on the sender wallet, the same test consistently lands at
exactly **0**, with the other 5 requests correctly rejected. Both
states are captured as an automated JUnit test in the repo.

**Reliable event publishing via the transactional outbox pattern.**
Every completed transfer writes a domain event into an `outbox_events`
table inside the *same* database transaction as its ledger entries. A
separate poller publishes these to Kafka and marks them sent only after
a confirmed publish. Verified by stopping the Kafka broker mid-test,
confirming transfers still succeed and their events sit safely
unpublished, then restarting Kafka and watching the events flush
automatically with zero manual intervention.

**Automated reconciliation for stuck transactions.**
A scheduled job periodically scans for transactions stuck in `PENDING`
past a threshold and resolves them based on what the ledger actually
shows — completing, reversing, or failing them as appropriate — so
nothing is ever left in limbo indefinitely.

**Redis-backed rate limiting.**
Transfer requests are limited per wallet using a Redis fixed-window
counter, protecting the API from abuse without needing in-memory state
that wouldn't scale across multiple instances.

## Tech stack

Java 17 · Spring Boot · Spring Data JPA · MySQL · Redis · Apache Kafka
· Docker · JUnit 5

## Architecture

```
Client
  │  POST /transfers  (Idempotency-Key header required)
  ▼
TransferController → IdempotencyService → RateLimiterService (Redis)
  ▼
TransferService  (single @Transactional boundary)
  ├─ locks sender wallet row (SELECT ... FOR UPDATE)
  ├─ validates balance via LedgerService (derived, not stored)
  ├─ writes DEBIT + CREDIT ledger entries
  └─ writes an OutboxEvent (same transaction)
  ▼
OutboxPoller (scheduled, every 5s) → Kafka topic: wallet-transactions
ReconciliationService (scheduled, every 15s) → resolves stuck PENDING transactions
```

## What I'd build next

- OAuth2-based user authentication, replacing the current plain-string
  `userId` with a real identity tied to authenticated sessions
- Multi-currency support with FX conversion
- A true token-bucket rate limiter (smoother burst handling than the
  current fixed-window approach) via a Redis Lua script

## Running it locally

Requires Java 17+, Maven, Docker Desktop, and a local MySQL instance.

```bash
docker compose up -d          # starts Kafka + Redis
mvn spring-boot:run           # connects to local MySQL automatically
```
