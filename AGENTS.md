# Eatzy backend agent guide

Communicate with the owner in Vietnamese. Explain changes and commands in plain language.
This repository is the backend source of truth for the Eatzy web and mobile clients.

## Start with the task

- Inspect the current branch and working tree; preserve existing user changes.
- Read the relevant source before changing behavior. Do not infer business rules from names alone.
- Complete authorized local edits and useful verification without repeated confirmation.
- Keep commits, pushes, server deployments and data operations within the user's actual request.
- Report what changed, checks performed, cross-repo impact and any remaining limitation.

## Repository map

- `settings.gradle.kts`, `build.gradle.kts`: Java 17, Spring Boot 3.4.3, Gradle wrapper; 11 executable services plus `eatzy-common`.
- `eatzy-api-gateway`: reactive gateway, public entry point on port 8080 and route table.
- `eatzy-auth-service`: users, roles, addresses, customer/driver profiles and JWT/cookie lifecycle.
- `eatzy-restaurant-service`: restaurants, menus, options, categories and reports.
- `eatzy-order-service`: order creation, pricing, state transitions, driver assignment and earnings.
- `eatzy-cart-service`, `eatzy-payment-service`: carts; payment strategies, wallets and vouchers.
- `eatzy-communication-service`: Kafka consumers, notification factories, STOMP chat/location.
- `eatzy-interaction-service`, `eatzy-system-config-service`: reviews/favorites/scoring; business settings.
- `eatzy-common`: response envelopes, Feign decoding/auth forwarding, events and shared helpers.
- `eatzy-config-server`, `eatzy-discovery-server`: centralized config and Eureka.
- `eatzy-ai-service`: separate Python FastAPI service; Java tests do not cover it.
- `docs/agent/architecture.md`: current contracts, implementation boundaries and known gaps.

## Skills

- `$eatzy-backend-change`: implement or extend backend behavior and coordinate client contracts.
- `$eatzy-backend-debug`: reproduce and diagnose API, Docker, auth or realtime failures.
- `$eatzy-backend-review`: review changes for concrete regressions and missing validation.

Skills live in `.agents/skills/`; their references use paths from the repository root.
Read only the skill and architecture sections relevant to the current task.

## Commands from the repository root

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode doctor
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode test -Service eatzy-order-service
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode artifacts
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode config
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode health
```

`test` without `-Service` runs the Java test suite. `artifacts` prepares Docker JARs.
Use `scripts/start-local.ps1` to build/start local Docker; `-SkipBuild` reuses images.
The wrapper is a convenience; inspect its behavior before choosing a mode.
Direct Gradle: `.\gradlew.bat :eatzy-order-service:test` or `./gradlew :eatzy-order-service:test`.
Use targeted meaningful tests for changed behavior. Broaden checks for shared code or unresolved risks.
Documentation-only changes usually need link/command review, not a full service rebuild.

## Contracts and boundaries

- Preserve Java `{statusCode,error,message,data}` envelopes, pagination and DTO field names unless a contract change is intentional.
- Trace controller → service/facade → mapper/repository/Feign client and the relevant event consumer.
- `FeignConfig`/`RestResponseDecoder` intentionally are not component-scanned; preserve response unwrapping and auth forwarding.
- Keep order status transitions in the existing state machine, monetary values in `BigDecimal` and server pricing checks intact.
- Extend existing payment strategies and Spring notification creators rather than duplicating dispatch logic.
- Do not claim ownership checks, role enforcement, payment idempotency or transactional event delivery without verifying implementation.
- Auth uses bearer access tokens and rotating HttpOnly Secure refresh cookies; mobile refresh behavior requires explicit integration evidence.

## Related repositories

Normally the sibling folders are `../eatzy_frontend` and `../eatzy_mobile`; locate the actual checkout if absent.
Read their `AGENTS.md` when working there. Web is Next.js/pnpm; mobile is React Native.
For API/DTO/auth/STOMP changes, identify affected web/mobile consumers and update them if in scope.
Record coordinated follow-up work explicitly when a companion checkout is unavailable.

## Local operations and release

`docker-compose.yml` is local development; `docker-compose.prod.yml` targets the physical server.
Keep local and production `.env`, DB/Redis addresses and signing keystores separate; do not print secret values.
GitHub Actions in `.github/workflows/cicd.yml` is active; push `main` publishes/deploys after checks.
Jenkins is paused; its previous pipeline lives in `ci/Jenkinsfile.jenkins`.
See `docs/agent/workflows.md`, `docs/docker-local.md` and `docs/main-ci-cd.md` for practical usage.
