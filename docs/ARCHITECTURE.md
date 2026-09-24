# NexusSphere — Architecture

## System Overview

NexusSphere is a capital-intelligence operating system for a Wealthsimple TFSA portfolio.
It combines quantitative signal scoring, risk-gated automated execution, and AI-assisted research.

```
Market Data / News
    ↓
Signal Engine (shared/models/)
    ↓
Risk Gate (shared/risk/ + server guardrails.py)
    ↓            ← score ≥ 65 required for auto-execution
Execution (Flask → SnapTrade → Wealthsimple)
    ↕
AI / Agents (Anthropic Claude + OpenAI via Flask proxy)
    ↕
Nexus UI (Compose Multiplatform: Android + Desktop)
```

---

## Module Map

| Module | Language | Responsibility |
|---|---|---|
| `shared/domain/` | Kotlin (commonMain) | Immutable value objects: Money, Symbol, Signal, OrderIntent, RiskDecision |
| `shared/models/` | Kotlin (commonMain) | Financial calculations: Kelly, Sharpe, VaR, SignalScore |
| `shared/risk/` | Kotlin (commonMain) | OrderGuard — deterministic policy mirror |
| `composeApp/` | Kotlin (Compose Multiplatform) | Android + Desktop UI |
| `server/` (Flask, `main.py`) | Python | SnapTrade proxy, kill switch enforcement, AI routing |
| `guardrails.py` | Python | Authoritative server-side order guard |
| `tests/unit/` | Python (pytest) | Financial model unit tests |
| `tests/execution/` | Python (pytest) | Order guardrail tests |
| `tests/integration/` | Python (pytest) | SnapTrade route integration tests |

---

## Security Boundaries

1. **Credentials in POST body only.** `userId` / `userSecret` must never appear in URL query parameters.
   Enforced by `_creds_from_request()` in Flask and by `snapBackend()` in the HTML frontend.

2. **Kill switch is server-authoritative.** `NS_KILL_SWITCH=true` blocks all orders at `guardrails.py`.
   The Kotlin `OrderGuard` mirrors this for client-side advisory checks only.

3. **Profit gate is enforced at two layers:**
   - Frontend (advisory): `Signal.PROFIT_GATE = 65`
   - Server (authoritative): strategy auto-fire is gated by Flask before calling SnapTrade

4. **No live execution from the client.** The KMP client submits `OrderIntent`; the server validates,
   previews (impact check), and only executes after explicit user confirmation.

---

## Trading Safety Flow

```
Client OrderIntent
    → POST /api/snap/order/impact   (preview, no execution)
    → User reviews OrderPreview
    → POST /api/snap/order/place    (execution, tradeId required)
         ↑
    guardrails.py: kill switch + notional cap + signal score check
         ↑
    SnapTrade SDK: place_order()
         ↑
    Wealthsimple TFSA
```

Auto-execution path (`/api/snap/order/force`) requires:
- Signal score ≥ 65
- Kill switch off
- Notional ≤ `NS_MAX_ORDER_CAD` (default $2,000 CAD)
- Kelly-sized position (recomputed each execution, never cached)

---

## Portfolio Context

| Field | Value |
|---|---|
| Account type | Wealthsimple TFSA |
| Holdings | AMD, BB, BB.TO, ENB, ORCL, PANW, PLTR, TSM |
| Base currency | CAD |
| CAD/USD rate | 0.736 |
| Max order cap | $2,000 CAD (env: `NS_MAX_ORDER_CAD`) |

---

## Phase Status

| Phase | Description | Status |
|---|---|---|
| 1 | KMP shell + shared domain + ADRs | ✅ Complete |
| 2 | Portfolio read-only screens (SnapTrade data) | Planned |
| 3 | Live market data feed | Planned |
| 4 | News / event intelligence pipeline | Planned |
| 5 | Risk module expansion (concentration, drawdown) | Planned |
| 6 | Research OS + hypothesis tracking | Planned |
| 7 | Paper trading | Planned |
| 8 | Authenticated order preview in KMP | Planned |
| 9 | Controlled live execution via KMP | Planned |
