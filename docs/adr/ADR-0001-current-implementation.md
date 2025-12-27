# ADR-0001: Transaction ledger design

* **Status**: Accepted
* **Date**: 2025-12-26
* **Issue**: #4
* **Owner**: Anton Serdyuchenko

This ADR documents both the current single-entry implementation and the target double-entry design, so the migration path stays explicit.

---

## 1. Context

This project is a learning **Bank** application. I want a core model that is:

* close to real banking mechanics,
* simple enough for TDD and refactoring,
* independent from a specific persistence technology (JPA, event store, etc.).

Right now:

* Accounts are domain objects with a stored balance.
* Transactions are single-account entries in an append-only ledger.
* Transfers are recorded as two correlated entries.

Target direction:

* Double-entry ledger with postings.
* Account balances derived from the ledger (projection).

---

## 2. Decision

### 2.1 Current (Phase 1)

* Keep `Account.balance` as the canonical current balance.
* Record operations in an append-only ledger as `Transaction` entries.
* Ledger entries are immutable after creation (no updates/deletes).
* Transfer is recorded as two entries (debit and credit) linked by a correlation id.
* Deposit/withdrawal are allowed as single-entry operations (implicit external counterparty).

### 2.2 Target (Phase 2+)

* Implement a double-entry ledger with `Transaction` + `Posting` aggregate.
* Ledger becomes the source of truth; balances are projections.
* Corrections are done via compensating transactions.
* Deposit/withdrawal use internal technical accounts to stay balanced.

---

## 3. Current model (as implemented)

> Names reflect current code in `src/main/java/com/serdyuchenko/bank`.

### 3.1 Core concepts

* **Account**

  * `requisite` (used as account id in ledger)
  * `balance` (stored value)
  * no `currency` or `status` yet

* **Transaction** (single-account ledger entry)

  * `id`
  * `accountId` (maps to `Account.requisite`)
  * `amount` (`Money`: currency + positive BigDecimal)
  * `type` (DEPOSIT, WITHDRAWAL, TRANSFER, FEE, etc.)
  * `timeStamp`
  * `metadata` (`TransactionMetadata` with `transactionId` + `description`)

* **TransactionLedger**

  * `record(accountId, type, amount, metadata)` returns a new `Transaction`
  * `getTransactions(accountId)` returns a snapshot list
  * append-only, per-account list

### 3.2 Example: transfer between accounts (current)

For a transfer `A -> B` with amount `100`:

* Generate a `transferId` (correlation id).
* Record entry for source account (type TRANSFER, amount 100, metadata.transactionId = transferId).
* Record entry for target account (type TRANSFER, amount 100, metadata.transactionId = transferId).
* Update balances directly on both accounts.

---

## 4. Current invariants (testable today)

1. **Immutability**

   * `Transaction` entries are immutable after creation.
   * Ledger is append-only; no updates or deletes.

2. **Basic transaction validation**

   * `id`, `accountId`, `type`, `amount`, `timeStamp` are non-null/blank.
   * `Money.amount > 0` and `Money.currency` is non-blank.

3. **Transfer correlation**

   * A transfer creates two entries with the same `metadata.transactionId`.
   * Transfer amount is equal on both entries.

4. **Account balance rules (current business logic)**

   * Deposits increase balance; withdrawals and transfers decrease balance.
   * Balance must not drop below zero (current policy in service layer).

---

## 5. Target model (double-entry ledger)

> This is the planned model; it is not fully implemented yet.

### 5.1 Core concepts

* **Account**

  * `accountId`
  * `currency`
  * `status` (active/closed)
  * (optional) `cachedBalance`

* **Transaction** (aggregate)

  * `transactionId`
  * `timestamp`
  * `type` (DEPOSIT, WITHDRAWAL, TRANSFER, FEE, etc.)
  * `description` (optional)
  * list of `Postings`

* **Posting** (ledger entry)

  * `postingId`
  * `transactionId`
  * `accountId`
  * `direction` (DEBIT / CREDIT)
  * `amount`
  * `currency` (must equal account currency)
  * `timestamp` (same or close to transaction timestamp)

### 5.2 Example: transfer between accounts (target)

For a transfer `A -> B` with amount `100 RUB`:

* Transaction `T1`:

  * Posting P1: `accountId = A`, `direction = DEBIT`, `amount = 100`
  * Posting P2: `accountId = B`, `direction = CREDIT`, `amount = 100`

Invariants:

* `sum(debit.amount) == sum(credit.amount) == 100`
* `currency(A) == currency(B) == RUB`
* `balance(A)` decreases by 100, `balance(B)` increases by 100 (via projection).

---

## 6. Target invariants (to implement)

1. **Immutability**

   * Once persisted, postings and transactions are not updated or deleted.
   * Corrections are done by new transactions.

2. **Balanced transactions**

   * For each transaction:

     * `sum(debit.amount) == sum(credit.amount)`
     * At least one debit and one credit posting exist.

3. **Currency safety**

   * All postings in a transaction share the same currency (for this project version).
   * Posting currency must equal account currency.

4. **Account consistency**

   * Posting can only refer to existing, active accounts.
   * Closed accounts are blocked from new postings.

5. **Idempotency**

   * Re-processing the same business command should be rejected as duplicate
     or return the same transaction (no double booking).
   * Implementation detail (idempotency keys) will be added later.

---

## 7. Consequences

### 7.1 Current model

**Positive**

* Simple and fast to iterate with TDD.
* Append-only history exists for audit/debugging.
* Works with minimal persistence setup.

**Negative / trade-offs**

* Ledger is not the source of truth for balances.
* No explicit debit/credit semantics in the ledger entries.
* Harder to reconcile and audit compared to double-entry.

### 7.2 Target model

**Positive**

* Full, auditable double-entry history.
* Balances can be reconstructed from ledger postings.
* Natural foundation for statements, reconciliation, and controls.

**Negative / trade-offs**

* More entities and rules to manage (`Transaction`, `Posting`).
* Requires balance projection and possibly snapshots for performance.
* Needs internal technical accounts for deposit/withdrawal.

---

## 8. Alternatives considered

### A. Implement double-entry now

* **Pros**: Immediate accounting correctness and ledger-as-source-of-truth.
* **Cons**: Larger refactor and more concepts to learn at once.
* **Decision**: Postponed to Phase 2+ to keep Phase 1 small and TDD-friendly.

### B. Keep only account balances (no ledger)

* **Pros**: Simplest possible implementation.
* **Cons**: No audit trail, harder debugging, no clear history of money movements.
* **Decision**: Rejected; even Phase 1 needs append-only history.

### C. Full event sourcing

* **Pros**: Powerful replay and time-travel capabilities.
* **Cons**: Higher complexity and infrastructure overhead.
* **Decision**: Out of scope for Phase 1; may be revisited later.

---

## 9. Evolution rules (ledger extensions)

* New `TransactionType` values must define whether they are single-entry (Phase 1) or double-entry (Phase 2+).
* Every new operation must document its invariant checks (balance rules, correlation id usage, etc.).
* `TransactionMetadata` can be extended with optional fields, but existing fields must remain backward compatible.
* Ledger entries remain append-only; corrections are modeled as new entries or compensating transactions.

---

## 10. Migration path / Next steps

* Keep current single-entry model during Phase 1.
* Ensure every multi-entry operation uses a correlation id.
* Introduce internal technical accounts for deposit/withdrawal (Phase 2).
* Add Posting + balanced Transaction aggregate (Phase 2+).
* Switch balance calculation to a projection model (Phase 2+).
