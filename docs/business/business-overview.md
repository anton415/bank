# Bank Project — Business Logic Overview

## Purpose
- Capture the functional intent of the banking side-project so the same content can seed the first Confluence space.
- Explain what the application currently does and what is coming next from a business perspective (architecture lives under `docs/architecture`).

## Product Vision (current iteration)
- Minimal personal banking back-office that supports a single customer interacting via CLI (HTTP later).
- Focus on correctness of money movements and auditability; UX and integrations come later.
- Use as a playground to learn banking rules, Java design, and later DevOps.

## Actors
- **Customer** — owns one or more accounts, initiates deposits and withdrawals.
- **System** — enforces validation rules, keeps balances and transaction history.

## Core Concepts
- **Account** — logical ledger bucket with an identifier and running balance.
- **Money** — value object; today tracks only amount, currency planned.
- **Transaction** — append-only statement entry persisted via the transaction ledger for bookkeeping and reconciliation.

## Current Capabilities
1. **Open account (implicit)** — when the system boots with seed data or when an account object is constructed.
2. **Deposit funds**
   - Amount must be greater than zero.
   - Successful operation increases the account balance by the amount.
   - Validation failure message: "Deposit amount must be greater than zero."
   - Success confirmation: "Deposit completed successfully."
3. **Withdraw funds**
   - Amount must be greater than zero.
   - Balance must remain non-negative; no overdraft is allowed yet.
   - Validation failure message: "Withdrawal amount must be greater than zero."
   - Insufficient funds message: "Insufficient funds; balance cannot go below zero."
   - Success confirmation: "Withdrawal completed successfully."
4. **Transfer funds**
   - Amount must be greater than zero and source balance must be sufficient (no overdraft).
   - Validation failure message: "Transfer amount must be greater than zero."
   - Insufficient funds message: "Insufficient funds; balance cannot go below zero."
   - Success confirmation: "Transfer completed successfully."
   - Emits paired ledger entries (debit + credit) that share a correlation id.
5. **Track append-only ledger (BANK-7 — “Track Append-Only Transactions”)**
   - Every deposit/withdrawal/transfer records an immutable `Transaction` with timestamp and metadata.
   - Metadata includes a generated identifier for idempotency/correlation and descriptive text (e.g., counterpart account).
   - Ledger history can be replayed per account for audit and troubleshooting.

## Business Rules & Constraints
- Every monetary amount is positive; zero or negative values are rejected before persisting.
- User receives explicit validation messaging for each operation so the CLI/service can surface the rules verbatim.
- Transactions are immutable; once recorded in the ledger they can’t be edited or removed.
- All operations must be idempotent when retried via future APIs (note for later HTTP layer).
- Audit trail reflects chronological order of events via the in-memory transaction ledger.

## Planned Enhancements
1. **Ledger queries & reconciliation tooling**
   - Provide CLI/REST endpoints to read ledger entries and reconstruct balances externally.
   - Surface correlation ids and metadata for customer-facing audit trails.
2. **Persistence layer**
   - Move from in-memory structures to PostgreSQL using JDBC or JPA.
   - Adopt optimistic locking on account aggregates to stop lost updates.
3. **External interfaces**
   - CLI flows first (text menu).
   - REST endpoints later with simple authentication.

## Recently Completed Work
- **BANK-7 — Track Append-Only Transactions**
  - Introduced `TransactionLedger` to capture every successful money movement with UUID-based identifiers.
  - `BankService` now records paired entries for transfers and attaches human-readable metadata for deposit/withdrawal origins.
  - Extended automated tests cover ledger recording plus metadata linking, ensuring regressions are caught early.

## Open Questions / Decisions to Track
- Do we support multi-currency accounts or separate account per currency?
- Approval rules for large withdrawals/transactions (manual review? limits?).
- Strategy for reversing a transaction (create compensating entry vs. delete).
- How to model fees and interest (compound schedule, daily calculation, etc.).

## Suggested Confluence Structure
- Home page: product vision + current capabilities (copy from this doc).
- Child pages:
  1. Customer Journeys (deposit, withdraw, transfer once ready).
  2. Business Rules Catalogue (validation, limits, audit requirements).
  3. Roadmap & Decisions (link to ADRs in GitHub).
