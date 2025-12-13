# ADR-0001: Transaction ledger design

* **Status**: Proposed
* **Date**: 2025-12-07
* **Issue**: #4
* **Owner**: Anton Serdyuchenko

---

## 1. Context

This project is a learning **Bank** application. I want a core model that is:

* close to real banking mechanics (double-entry style),
* simple enough for TDD and refactoring,
* independent from a specific persistence technology (JPA, event store, etc.).

Right now:

* Accounts are (or will be) represented as domain objects.
* I need a clear way to **record all monetary movements** between accounts.
* Future use cases: deposit, withdrawal, transfer, statement, audit, reporting.

Key questions:

* How do I model **source of truth** for money movements?
* How do I compute balances and keep them consistent?
* How do I handle corrections (reversals) without deleting history?

---

## 2. Decision

I will implement a **double-entry, append-only transaction ledger** as the primary source of truth for all monetary movements.

High-level decisions:

1. **Ledger as append-only log**

   * A `Ledger` is a collection of immutable `LedgerEntry` (or `Posting`) records.
   * New business operations **only append** entries; no updates or deletes.
   * Corrections are done via **compensating transactions**, not by editing history.

2. **Double-entry postings per transaction**

   * A business operation is represented by a `Transaction` object.
   * Each `Transaction` produces **at least two postings**:

     * one debit posting,
     * one credit posting.
   * The total debit amount **must equal** the total credit amount (in the same currency).

3. **Account balance as a projection**

   * An `Account` balance is derived as:

     * `balance = sum(postings.amount * posting.direction)`
       for all postings of that account.
   * Optionally, I may keep a **cached balance** on the account for performance,
     but the ledger remains the source of truth.

4. **Invariants enforced in domain code**

   * Invariants are checked at domain level (before persistence):

     * no transaction with `amount <= 0`,
     * matching debit/credit totals,
     * account must be active, currency must match, etc.
   * Persistence (DB, JPA) must **not** break these invariants.

5. **Neutral to persistence technology**

   * The ledger model (entities/aggregates) does not depend on a specific DB.
   * The exact persistence strategy (single table, journal tables, event store)
     will be explored in a separate SPIKE (`#5`).

---

## 3. Ledger model (sketch)

> This section is intentionally implementation-agnostic. Concrete class names can change.

### 3.1 Core concepts

* **Account**

  * `accountId`
  * `currency`
  * `status` (active/closed)
  * (optional) `cachedBalance`

* **Transaction**

  * `transactionId`
  * `timestamp`
  * `type` (DEPOSIT, WITHDRAWAL, TRANSFER, FEE, etc.)
  * `description` (optional)
  * list of `Postings`

* **Posting** (Ledger entry)

  * `postingId`
  * `transactionId`
  * `accountId`
  * `direction` (DEBIT / CREDIT)
  * `amount`
  * `currency` (must equal account currency)
  * `timestamp` (same or close to transaction timestamp)

### 3.2 Example: transfer between accounts

For a transfer `A -> B` with amount `100 RUB`:

* Transaction `T1`:

  * Posting P1: `accountId = A`, `direction = DEBIT`, `amount = 100`
  * Posting P2: `accountId = B`, `direction = CREDIT`, `amount = 100`

Invariants:

* `sum(debit.amount) == sum(credit.amount) == 100`
* `currency(A) == currency(B) == RUB`
* `balance(A)` decreases by 100, `balance(B)` increases by 100.

---

## 4. Invariants

The ledger must guarantee the following invariants:

1. **Immutability**

   * Once persisted, postings and transactions are not updated or deleted.
   * Corrections are done by new transactions.

2. **Balanced transactions**

   * For each transaction:

     * `sum(debit.amount) == sum(credit.amount)`
     * At least one debit and one credit posting exist.

3. **Currency safety**

   * All postings in a transaction share the same currency
     (for this simple version of the Bank project).

4. **Account consistency**

   * Posting can only refer to **existing, active** accounts.
   * Closed accounts may be blocked from new postings.

5. **Idempotency**

   * Re-processing the same business command should either:

     * be rejected as duplicate, or
     * result in the same transaction being returned (no double booking).
   * Implementation detail (idempotency keys) may be added later.

---

## 5. Consequences

### 5.1 Positive

* Clear, auditable history of all monetary movements.
* Easy to derive balances, statements and reports from postings.
* Model is close to real banking double-entry accounting.
* Good foundation for later features:

  * statements,
  * reconciliation,
  * limits/controls.

### 5.2 Negative / trade-offs

* Computing balances from history can be slower for large datasets.

  * May require cached balances or periodic snapshots later.
* More entities to manage (`Transaction`, `Posting`) compared to a simple
  `balance` field on `Account`.
* For cross-currency operations I will need additional concepts (FX rates, etc.)
  which are **out of scope for now**.

### 5.3 Risks / open questions

* How exactly to implement persistence:

  * a single `ledger_entries` table vs. multiple tables,
  * JPA mapping strategy,
  * migrations of ledger schema.
* How to model **reversal/correction** flows:

  * explicit `REVERSAL` transaction type,
  * or links between correcting and original transactions.
* Do I need **separate ledgers** (customer accounts vs. internal technical accounts)?

These questions will be explored in future ADRs and/or SPIKE `#5`.

---

## 6. Alternatives considered

### A. Simple balance on Account only

* **Idea**: store just `balance` on `Account` and update it directly.
* **Pros**:

  * Very simple implementation.
* **Cons**:

  * No detailed history, no full audit trail.
  * Harder to debug issues and implement real banking behaviour.
* **Decision**: **Rejected** for this project.

### B. Full event-sourcing with domain events

* **Idea**: represent every change as domain events (`MoneyDeposited`, etc.)
  stored in an event store; build all read models via projections.
* **Pros**:

  * Very flexible, powerful for complex systems.
  * Nice for temporal queries and experiments.
* **Cons**:

  * More complex infrastructure and patterns to learn at once.
  * Heavier than needed for the current learning goals.
* **Decision**: **Postponed**. I may revisit event-sourcing later on top
  of the ledger model if the project grows.

---

## 7. Implementation notes / Next steps

1. Create domain classes for `Transaction` and `Posting` according to this ADR.
2. Add unit tests that check:

   * balanced transactions,
   * correct balance updates for deposit/withdraw/transfer.
3. Adapt the **"Implement transfer between accounts use case"** to use
   the ledger model.
4. Document this ADR in the project Wiki (short link / summary).
