# ADR-0001 — Adopt Kotlin Multiplatform for NexusSphere

**Date:** 2026-09-24  
**Status:** Accepted  
**Deciders:** Creova Engineering

---

## Context

NexusSphere started as a single-file Flask + HTML trading terminal hosted on Replit.
All financial intelligence, signal scoring, and UI logic were embedded in a 22 k-line
`static_nexussphere.html` file. This created three structural problems:

1. **Not locally reproducible.** The HTML file lived only on Replit's filesystem
   and was excluded from Git via `.gitignore`, making the product unreproducible
   from source control.

2. **No native mobile experience.** The web UI is not optimised for Android,
   which is the primary client target.

3. **Business logic mixed with presentation.** Kelly Criterion, Sharpe Ratio,
   VaR, and the Combined Signal Score were implemented as JavaScript inside the HTML
   with no unit tests and no version control.

---

## Decision

Migrate NexusSphere to a **Kotlin Multiplatform (KMP) + Compose Multiplatform** architecture
using a strangler-fig migration across 9 phases.

**Targets for Phase 1:** Android + JVM Desktop.  
**iOS deferred** until a macOS build environment is available.

The existing Flask server remains active as a backend adapter during migration.
SnapTrade brokerage integration stays in Python (no Kotlin SDK exists).

---

## Architecture Layers

```
Nexus UI (Compose Multiplatform)
    ↓
composeApp/ (Android + Desktop screens)
    ↓
shared/ (domain, models, risk — commonMain)
    ↓
server/ (Flask adapter → future Ktor server)
    ↓
SnapTrade SDK (Python, authority for live execution)
    ↓
Wealthsimple TFSA
```

---

## Consequences

**Positive:**
- Financial models are now versioned, unit-tested Kotlin in `shared/`.
- Android app can be built and distributed without Replit.
- Desktop app supports offline research and back-testing.
- `shared/` domain models are the single source of truth for financial logic.

**Negative / Trade-offs:**
- Migration is multi-phase (9 phases, estimated 6–12 months full completion).
- Two codebases run in parallel during migration (HTML frontend + KMP).
- iOS requires a macOS build machine; excluded from Phase 1.

---

## Alternatives Considered

| Option | Rejected because |
|---|---|
| React Native | No shared Kotlin models; financial math would remain in JS |
| Flutter | Dart ecosystem has no financial library equivalents; Kotlin is team's primary language |
| PWA only | Doesn't solve local reproducibility or native distribution |
| Rewrite in full | Too risky; strangler-fig preserves working Flask server and existing tests |
