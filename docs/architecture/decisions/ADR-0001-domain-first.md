---
adr: 0001
title: Domain-first
date: 2025-10-25
status: Accepted
---

## Context
- We are building a learning project that models core banking behaviours (e.g., `Account`, deposits, withdrawals) before adding delivery mechanisms such as REST or persistence.
- The first deliverables are domain tests and entities; infrastructure concerns (web, database, messaging) will arrive later or may evolve as experiments.
- Coupling business logic to frameworks too early would slow iteration, complicate testing, and make later refactors (such as moving from CLI to HTTP) risky.

## Decision
- Keep the domain model in plain Java classes within the `domain` package; avoid framework annotations or lifecycle hooks in this layer.
- Model behaviour through value objects and entities that enforce invariants (e.g., positive amounts) and support TDD-driven development.
- Introduce application services and adapters only when needed, each in their respective packages (`app`, `adapters`), and have them depend on the domain abstractions.
- Use ports (interfaces) in the domain/app layers to describe external dependencies (e.g., repositories), with infrastructure-specific implementations living in adapters.

## Rationale
- A clean domain core is easier to unit test and reason about, allowing us to validate banking rules without bootstrapping infrastructure.
- Separating concerns now keeps the codebase ready for future module splits (e.g., moving adapters into new Gradle/Maven modules).
- The approach mirrors practices from Domain-Driven Design and hexagonal architecture, giving a familiar structure for collaborators and future contributions.

## Consequences
**Positive**
- Rapid iteration on domain rules with minimal setup.
- Easier onboarding and experimentation because the domain code has no hidden framework dependencies.
- Flexibility to add delivery mechanisms (HTTP, CLI, batch) by building new adapters on top of the same domain core.

**Negative / Mitigations**
- Requires discipline to keep frameworks out of the domain; mitigate with code reviews and linting guidelines.
- Some duplication in early stages (e.g., manual wiring) until we introduce a DI mechanism; acceptable trade-off for clarity.
- Additional documentation effort to explain port/adapters boundaries; addressed by the architecture docs and diagrams.
