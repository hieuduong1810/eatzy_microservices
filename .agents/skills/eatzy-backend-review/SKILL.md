---
name: eatzy-backend-review
description: Review Eatzy backend changes for actionable regressions in API compatibility, authorization, order/payment rules, events and deployment behavior; use for requested reviews rather than ordinary implementation.
---

# Eatzy backend review

Read `AGENTS.md` and relevant sections of `docs/agent/architecture.md`. Respond in Vietnamese. Inspect the requested diff/base and nearby callers/tests; do not modify code when the user requested review only.

## Follow the impact

- Trace changed controller/DTO → mapper/service/facade → repository/Feign → events/consumer, not just edited lines.
- Check `eatzy-common` changes against all affected services; preserve envelope/pagination, Feign unwrapping, auth forwarding and JSON fields.
- Search route/field/notification consumers in sibling web/mobile checkouts when present. Report a concrete compatibility break, not a speculative possibility without evidence.
- Verify ownership/role checks at the backend boundary. Gateway `permitAll`, UI guards and request customer IDs do not establish authorization.
- Review auth rotation/logout, cookie flags and token claims against existing clients. JWT roles are not represented the same way as login user.role.
- For orders, inspect transition legality, price recalculation, positive quantities, rounding and transaction side effects.
- For payment/wallet/callback/refund changes, consider duplicate execution, failure ordering, balances and signature checks. Do not claim existing idempotency without source evidence.
- For Kafka/STOMP changes, check topic/schema/destination compatibility, principal/recipient routing and behavior after reconnect/retry. DB rollback does not retract Kafka/remote writes.
- For Docker/CI edits, check JAR preparation, local/prod separation, keystore/env paths and main-only release conditions. GitHub Actions is active; Jenkins is paused.

Distinguish pre-existing limitations from regressions introduced by the diff. Existing gaps become findings when the reviewed change worsens them or depends on an unsupported guarantee. Avoid style-only feedback and broad redesign requests.

## Evidence and validation

Use read-only source tracing first. Run focused existing tests or a non-destructive reproduction when they materially establish a finding and fit the review scope. Documentation-only reviews rarely need a full build. If checks are not run, say so; 23 narrow Java tests do not establish payment/realtime/AI coverage.

Present actionable findings in priority order, each with the actual file/line, trigger and consequence. Include only issues supported by a plausible concrete path through the implementation. If no actionable findings remain, state that and summarize meaningful test gaps or unverified runtime behavior briefly.

Do not silently push, merge or deploy as part of a review. If the user also requested fixes, complete the authorized fixes and validate the changed behavior, preserving existing unrelated edits.
