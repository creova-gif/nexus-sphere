# ADR-0002 — Retain Flask as Parallel Adapter During KMP Migration

**Date:** 2026-09-24  
**Status:** Accepted  
**Deciders:** Creova Engineering

---

## Context

The KMP migration (ADR-0001) replaces the client layer incrementally.
The server layer (`main.py`, Flask) currently:

- Proxies all SnapTrade SDK calls (Python-only SDK, no Kotlin equivalent)
- Enforces server-side order guardrails (`guardrails.py`)
- Routes AI inference via Anthropic/OpenAI proxies
- Manages user credential flow (`_creds_from_request()`)

---

## Decision

**Keep Flask running as a parallel adapter** for the full duration of the KMP migration.

Ktor server adoption is deferred to Phase 6–7, after:
- Shared domain modules are stable and tested
- A Kotlin HTTP client for SnapTrade has been evaluated or an alternative broker API found
- Server-side risk policies have been ported to `shared/risk/`

During migration, KMP clients call Flask over HTTP using Ktor client.

---

## Rationale

| Factor | Detail |
|---|---|
| SnapTrade SDK | Python-only. No Kotlin client exists. Flask cannot be fully replaced until the broker integration layer changes. |
| Risk continuity | `guardrails.py` is the authoritative kill switch. Replacing it mid-migration adds risk. |
| Migration speed | Retaining Flask allows Phase 1–5 to focus on client and domain, not server re-architecture. |
| Test coverage | Flask server has 30+ existing tests. These continue to run in CI. |

---

## Consequences

- Flask + KMP run simultaneously during migration phases 1–5.
- All new financial calculation logic is written in `shared/` Kotlin first.
- Flask is considered deprecated once Ktor server is certified to pass the same guardrail test suite.
- No new business logic may be added to `main.py`; new logic goes into `shared/` or `server/` (future Ktor).
