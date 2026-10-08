# TrackMyFinance

### Dependencies to add:

Spring Web: REST controllers

Spring Data JPA: entities and repositories

Validation: @Valid, @NotNull, @Positive on request DTOs

Spring Security: password hashing and JWT filter chain

MySQL Driver: JDBC driver

Flyway Migration: versioned schema (this is the dependency that pulls in the Flyway starter)

Testcontainers

### Daywise Plan

- 1-2	MySQL, Flyway schema, entities, repositories, tests

- 3	Register, login, JWT, /me

- 4	Expense CRUD, categories, consistent error format, per-user privacy

- 5	Dashboard summary (totals, category shares, daily totals, period comparison)
