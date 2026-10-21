---
name: eatzy-backend-debug
description: Diagnose and fix reproducible Eatzy API, auth, Gradle, Docker or Kafka/STOMP failures using source and runtime evidence; distinguish local configuration from production incidents.
---

# Eatzy backend debug

Read `AGENTS.md` and only the relevant parts of `docs/agent/architecture.md`. Explain findings and next verification in Vietnamese. Preserve the current working tree.

## Establish the failing boundary

Collect the command/request, URL, expected result, actual status/error and environment. Distinguish local Compose, individual JVMs and deployed production. Existing source is not proof of which commit is running. Inspect the smallest relevant logs and sanitize credentials, cookie/token values and personal data.

For environment/build failures, start with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode doctor
```

For a running Docker stack use `docker compose ps`, `-Mode health`, and bounded logs for the failing service. For Compose syntax use `-Mode config`; do not print fully interpolated config containing secrets.

## Follow the symptom

| Symptom | Useful checks |
|---|---|
| Docker COPY JAR missing | Java Dockerfiles consume `docker-artifacts/<service>.jar`; `-Mode artifacts` builds the 11 executable JARs |
| Java/Gradle failure | Confirm JDK 17 and wrapper; identify compile/test error before rebuilding images |
| Gateway error | Route and downstream service/Eureka health; gateway health alone is insufficient |
| DB/Redis connectivity | Local Compose targets host MariaDB:3307 and Docker Redis by default; container localhost refers to itself |
| Login/refresh fails | Actual API origin, Set-Cookie flags, cookie send behavior, access token expiry, rotation and DB refresh-token match |
| Mobile cannot connect | Device/emulator gateway host, network access and Secure-cookie/HTTPS behavior; Next `/api/proxy` is web-specific |
| STOMP connects but receives nothing | CONNECT auth header, principal email, subscription `/user/queue/...`, producer/topic/consumer and notification destination |
| Price or transition rejected | Server fee quote, dish/options/quantity, `OrderStatus` and `OrderStateMachine` |

Source contracts: `AuthController`, `SecurityUtil`, shared `FormatRestResponse`/`RestResponseDecoder`, gateway `application.yml`, communication `WebSocketAuthChannelInterceptor` and `NotificationFactory`. Read only the needed path. For Kafka trace publisher → common DTO → consumer → side effect; topic names with hyphens/underscores can intentionally differ.

## Fix and close the loop

Reproduce the failure without destructive data resets or broad service restarts. Distinguish the root cause from incidental Logstash/Eureka warnings. Apply the narrowest fix consistent with the contract, preserving meaningful validation and security behavior.

Run the failing check again and the appropriate service test (`-Mode test -Service <service>`). Use integration checks when a unit test cannot cover the boundary. Avoid real payment, email, production data mutations or deployments unless included in the task. When evidence requires unavailable credentials/devices/server access, state exactly what remains unverified and continue independent local work.

Report the cause, change, before/after evidence and remaining limitation. A health response is connectivity evidence, not proof of checkout, wallet settlement, permissions or realtime correctness.
