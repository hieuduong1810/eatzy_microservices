---
name: eatzy-backend-change
description: Implement or extend Eatzy Spring backend or AI service behavior, preserving existing API, pricing, auth and event contracts and coordinating affected web/mobile consumers.
---

# Eatzy backend change

Work from the backend repository root. Read `AGENTS.md` and the relevant sections of `docs/agent/architecture.md`; communicate progress and results in Vietnamese.

## Trace the actual behavior

Identify the route, request/response DTO, mapper, service/facade, persistence, Feign adapters and event/socket consumers involved. Inspect current tests and working-tree changes. For shared code changes, search its callers before editing.

Locate sibling `../eatzy_frontend` and `../eatzy_mobile` when the change affects clients. Confirm their actual location and instructions rather than assuming an unavailable checkout exists. Use the backend source as the current contract; record a proposed contract change explicitly.

## Implement at the existing boundary

- Preserve Java response envelopes and pagination; Python AI responses have a separate shape.
- Preserve auth forwarding and `RestResponseDecoder` behavior; `FeignConfig` is intentionally not component-scanned.
- Keep order creation in the facade/validation chain and transitions in the state machine. Keep monetary calculations in `BigDecimal` and server-calculated prices/fees.
- Extend payment strategy beans and notification creator components instead of duplicating dispatch logic.
- For permission-sensitive changes, verify ownership from authenticated identity at the relevant service boundary. Existing role guards or request IDs alone are not evidence of enforcement.
- For events/payments, trace failure, retry and transaction boundaries. A local DB rollback does not undo Kafka messages or another service's payment side effect.
- Keep local and production environment configuration separate. Do not alter CI providers or server deployment unless the task includes it.

Avoid unrelated architecture rewrites. If a desired feature needs API/auth changes for mobile, implement the agreed contract or clearly identify the gap; do not silently weaken production cookies or invent a refresh endpoint in the client. When the user requests a native auth contract, implement and test it explicitly; keep local-only cookie exceptions scoped to local configuration and preserve the web flow.

## Verify the changed behavior

Use the smallest meaningful check first:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode test -Service eatzy-order-service
```

Choose the actual service. For common/cross-service changes, use `-Mode test` without `-Service`. Add regression tests for behavior and invalid/unauthorized cases when warranted. Java tests do not exercise Python AI or prove end-to-end payment/realtime behavior.

Use `-Mode artifacts` when executable JAR/Docker compatibility needs checking, `-Mode config` for Compose edits, and `-Mode health` for a running stack. Rebuild/start services only when needed for the requested work. Run an appropriate focused API/socket smoke check for integration changes; report missing credentials/device/infrastructure as a limit rather than claiming the flow passed.

Review the final diff for unintended changes and update relevant architecture/consumer docs when contracts change. Report affected files, observable behavior, checks and cross-repo follow-up. Complete authorized local work; commit/push/deploy only within the user's actual scope.
