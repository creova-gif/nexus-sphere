# NexusSphere — KMP Migration Plan

## Strategy: Strangler Fig

The HTML frontend and Flask server remain fully operational throughout migration.
Each KMP phase wraps and replaces one subsystem at a time.
No phase is complete until its tests pass and the replaced subsystem is deprecated.

---

## Phase 1 — KMP Shell + Shared Domain ✅

**Deliverables:**
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`
- `shared/build.gradle.kts`, `composeApp/build.gradle.kts`
- `shared/domain/` — Money, Symbol, Signal, OrderIntent, RiskDecision, KillSwitchState
- `shared/models/` — KellyCriterion, SharpeRatio, HistoricalVar, SignalScore
- `shared/risk/` — OrderGuard (mirrors guardrails.py)
- `composeApp/` — Android + Desktop shell with navigation
- `commonTest/` — Kotlin unit tests for all models
- ADR-0001, ADR-0002
- `docs/ARCHITECTURE.md`, `docs/MIGRATION_PLAN.md`

**Definition of done:**
- `./gradlew :shared:allTests` passes
- `./gradlew :composeApp:assembleDebug` produces a buildable Android APK
- `./gradlew :composeApp:run` launches the desktop app
- No secrets in source
- ADRs written

---

## Phase 2 — Portfolio Read-Only Screens

**Goal:** Replace the positions/holdings tab in the HTML frontend with a native KMP screen.

**Work items:**
- Ktor client integration for Flask API calls
- `PortfolioRepository` interface in `shared/`
- Android + Desktop positions screen in Compose
- SnapTrade auth flow in KMP (user registration + broker connection)
- Demo/offline fallback data

**Dependencies:** Phase 1 complete

---

## Phase 3 — Market Data

**Goal:** Replace seeded pseudo-random prices with a real market data feed.

**Work items:**
- Evaluate data provider (Alpaca, Polygon.io, Yahoo Finance unofficial)
- `MarketDataRepository` interface + Ktor client implementation
- Wire real prices into `SignalScore.evaluate()`
- Replace demo `MarketSnapshot` generation

**Dependencies:** Phase 2 complete

---

## Phase 4 — News / Event Intelligence

**Goal:** News pipeline beyond LLM sentiment.

**Work items:**
- `NewsEvent` domain model
- Source ingestion (RSS, financial news APIs)
- Entity resolution → symbol mapping
- Materiality scoring
- Signal contribution from news

**Dependencies:** Phase 3 (real market data needed for reaction measurement)

---

## Phase 5 — Risk Module Expansion

**Goal:** Move additional risk checks from Flask into `shared/risk/` and enforce server-side.

**Work items:**
- Position concentration limits (per-symbol, per-sector)
- Daily loss limit
- Drawdown circuit breaker
- Duplicate order protection
- Stale quote rejection
- Expand `KillSwitchState` enforcement in Flask

**Dependencies:** Phase 2 (live portfolio positions needed)

---

## Phase 6 — Research OS

**Goal:** Traceable research lineage for every strategy.

**Work items:**
- `research/hypotheses/`, `research/experiments/`, `research/backtests/`
- ID scheme: HYP-YYYY-NNN, EXP-YYYY-NNN, STRAT-YYYY-NNN
- Strategy lifecycle state machine (DRAFT → LIVE → RETIRED)
- Backtest runner

**Dependencies:** Phase 3 (real historical data)

---

## Phase 7 — Paper Trading

**Goal:** Full strategy lifecycle in paper mode before live execution.

**Work items:**
- Paper portfolio simulation
- Strategy auto-evaluation with real signals
- P&L tracking without real orders
- Shadow mode vs live comparison

**Dependencies:** Phase 5 + 6

---

## Phase 8 — Authenticated Order Preview in KMP

**Goal:** Replace HTML order flow with native KMP preview screen.

**Work items:**
- KMP order intent builder
- Native preview screen (impact check result)
- Biometric confirmation on Android
- Integration with `OrderGuard` for client-side pre-validation

**Dependencies:** Phase 2 (auth), Phase 5 (risk)

---

## Phase 9 — Controlled Live Execution via KMP

**Goal:** KMP becomes the primary client for live trading.

**Work items:**
- Production-grade error handling (partial fills, broker failures, network retries)
- Audit log viewer
- HTML frontend formally deprecated (behind feature flag)

**Dependencies:** Phase 7 + 8 complete, paper trading validated

---

## Flask → Ktor Migration (Post Phase 9)

Once all phases complete and the KMP client is the primary interface:
- Port `guardrails.py` logic to `shared/risk/` Kotlin (already started)
- Implement Ktor server in `server/` with same API surface as Flask
- Run Ktor alongside Flask, verify all guardrail tests pass against Ktor
- Deprecate Flask
