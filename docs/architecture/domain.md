# Domain Model (current)

- Related: [ADR-0001 — Domain-first](./decisions/ADR-0001-domain-first.md)

## Glossary
- **Account** — holds a balance for a customer; supports deposit/withdraw.  
- **Money** — value object with `amount` and (later) `currency`.  
- **Transaction** — append-only record of money movement (planned).

## Account (exists)
**State (today):**
- id: `AccountId`
- balance: number (later `Money`)

**Operations (tests drive behavior):**
- `deposit(amount > 0)` → increases balance
- `withdraw(amount > 0, balance >= amount)` → decreases balance

**Invariants:**
- Amounts must be positive
- (Planned) No overdraft unless explicitly allowed

## BankService (exists)
- Holds user -> account relationships in-memory.
- Provides `depositFunds`, `withdrawFunds`, and `transferMoney` orchestration methods.
- Each operation returns an `OperationResult` that carries a success flag, the user-facing message defined in the business doc, and (when successful) the resulting balance.
- Validation order: locate account(s) → assert positive amount → assert sufficient funds (withdraw/transfer) → mutate state.
