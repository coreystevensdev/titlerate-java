# TitleRate (Java)

![CI](https://github.com/coreystevensdev/titlerate-java/actions/workflows/ci.yml/badge.svg)
![20 tests](https://img.shields.io/badge/tests-20-brightgreen)

Title insurance premium calculator for PA and NJ. Spring Boot 3.3, Spring Security stateless JWT, Spring Data JPA, PostgreSQL. 20 tests (JUnit 5 + MockMvc). No persistent live URL: run locally with `docker compose up` or deploy via Terraform to ECS Fargate (see `infra/`).

## Problem

Title insurance premiums follow state-filed tiered rate schedules with different rates at different property value bands. Lender policies issued simultaneously with owner policies receive a discount. Quoting the correct premium requires looking up the applicable tier from the filed schedule.

## Solution

A REST API that walks the full tier list for a given state and policy type, accumulating premium across rate brackets. Each tier covers a property value band; only the amount within that band is rated at the tier's `ratePerThousand`. The final `basePremium` is the sum across all tiers (the same math as tax bracket accumulation). Simultaneous issue discount applies to the accumulated base.

## Architecture

```mermaid
flowchart TD
    A["POST /api/calculate\npublic, no auth required"] --> B[PremiumCalculationService]
    B --> C["RateScheduleRepository\nfindByStateAndPolicyTypeOrderByTierStart()"]
    C --> D["Walk tiers: amountInTier = min(remaining, tierCapacity)\ntierPremium = amountInTier / 1000 * ratePerThousand"]
    D --> E["basePremium = sum of tierPremiums\ndiscount = basePremium * simultaneousDiscountPct"]
    E --> F["PremiumResponse\n{basePremium, simultaneousDiscount, netPremium, breakdown}"]
```

Open-ended final tiers (null `tierEnd`) absorb all remaining amount. The `breakdown` field in the response lists each tier's contribution for auditability.

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Runtime | Spring Boot 3.3, Java 19 | Familiar enterprise Java stack; JPA + Hibernate for schema management |
| Auth | Spring Security stateless + JJWT 0.12.6 | `OncePerRequestFilter` validates Bearer tokens; no session state |
| ORM | Spring Data JPA + Hibernate | JPQL tier query; `schema-create` in H2, `update` in prod |
| Passwords | BCrypt (Spring Security) | Industry-standard hashing with cost factor |
| Tests | JUnit 5, MockMvc, `@SpringBootTest` | Integration tests hit in-memory H2; `@Sql(test-rates.sql)` seeds each test class with DELETE + INSERT for idempotency |
| DB | PostgreSQL 16 (prod), H2 (test) | H2 in-memory for fast tests; Postgres for prod with the same schema |
| Infrastructure | AWS ECS Fargate, RDS PostgreSQL 17, ALB | Zero EC2 management; deployment circuit breaker rolls back on health-check failure |
| IaC | Terraform 1.9, GitHub Actions OIDC | Reproducible infra; CI deploys via short-lived IAM role, no stored AWS credentials |

## Getting Started

```bash
cp .env.example .env      # fill in JWT_SECRET
docker compose up -d db   # start PostgreSQL on :5434
./mvnw spring-boot:run    # starts on :8080
```

Calculate a PA lender policy for $150,000:

```bash
curl -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"state":"PA","policyType":"LENDER","amount":150000,"simultaneousIssue":false}'
```

Register and get a JWT:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

The only endpoint that actually requires the JWT is `GET /api/auth/me`, which resolves the caller's own profile from the token's role claim, never from a request parameter:

```bash
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <token>"
```

Or with Docker Compose:

```bash
cp .env.example .env
docker compose up
```

## Running Tests

```bash
./mvnw test
```

## Known Limitations

- Rate schedules are illustrative tiers for PA and NJ. Verify against current state filings before production use.
- H2 does not support `ON CONFLICT DO NOTHING`; the test seed uses DELETE + INSERT for idempotency. Re-ingesting rate schedules on a running prod DB requires idempotent upsert logic.
- No rate schedule update API; changes require a migration or seed update.
- Multi-state test coverage is limited: only PA tiers are seeded in the current test fixtures.

## License

MIT
