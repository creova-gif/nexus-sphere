# NexusSphere — Engineering Rules

## Architecture

- **Flask (`main.py`)** — thin API proxy only. No business logic, no trading decisions, no price calculations here.
- **Frontend (`static_nexussphere.html`)** — all UI, signal scoring, strategy evaluation, and portfolio math live here. This 22 k-line file is intentionally a single-file SPA for zero-dependency portability.
- **SnapTrade SDK** — all brokerage connectivity. Never bypass it with raw HTTP calls.
- **AI routes (`/api/ai/*`)** — proxy to Anthropic/OpenAI via Replit AI Integrations env vars only.

### Layers (data flows downward; never upward)
```
Market Data / News  →  Signal Engine  →  Risk Gate  →  Execution
                                            ↑
                                     profit-gate (score ≥ 65)
```

## Security

- **Credentials in POST body only.** `userId` and `userSecret` must NEVER appear in URL query params. `_creds_from_request()` in `main.py` enforces this — do not add endpoints that bypass it.
- **CORS** is set to `*` for development. Before production, restrict to the deployment domain.
- **No secrets in source.** All keys come from environment variables: `SNAPTRADE_CLIENT_ID`, `SNAPTRADE_CONSUMER_KEY`, `AI_INTEGRATIONS_*`.
- **Kill switch** — set `NS_KILL_SWITCH=true` in env to block all order endpoints immediately.
- **Max order cap** — `NS_MAX_ORDER_CAD` (default 2000 CAD) enforced server-side before every order reaches SnapTrade. The frontend warning is advisory only; the backend cap is the hard limit.

## Trading Safety Rules

1. The preview path (`/api/snap/order/impact` → `/api/snap/order/place`) is the required path for all user-initiated orders.
2. `/api/snap/order/force` is available for automated strategies only and is subject to the same `_order_guard()` checks (kill switch + max cap).
3. Auto-execution requires **signal score ≥ 65** (profit gate). A strategy at score 55–64 arms but does not fire.
4. Kelly Criterion position sizing must be recomputed on each auto-execution, not cached.
5. DCA strategies ignore the profit gate (score condition always true) — they are scheduled contributions, not momentum trades.

## Model Validation Standards

- Every signal model change must include a back-test result showing positive expected value.
- Sharpe ratio, max drawdown, and win rate must be logged before any model is promoted to auto-execution.
- The Combined Signal Score weights (RSI, MACD, Trend, Stage, DCF, Fundamentals, Momentum) may only be adjusted after a documented back-test comparison.

## Testing Requirements

- All financial math (Kelly, Sharpe, VaR, DCF) must have unit tests in `tests/unit/`.
- All SnapTrade proxy routes must have integration tests in `tests/integration/` using mocked SDK responses.
- Order guardrail logic must be covered in `tests/execution/test_order_guardrails.py`.
- CI must run `pytest` on every push to the main branch.

## Forbidden Actions

- Do not add `userId`/`userSecret` to query strings.
- Do not disable `_order_guard()` without a documented reason and PR review.
- Do not hardcode prices, positions, or portfolio values — all data must come from SnapTrade or the frontend's live calculations.
- Do not commit `.env` files or any file containing API keys.
- Do not call `client.trading.place_force_order()` from any route that is not gated by `_order_guard()`.
- Do not introduce `eval()` or `exec()` in any module.

## Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `SNAPTRADE_CLIENT_ID` | SnapTrade app ID | required |
| `SNAPTRADE_CONSUMER_KEY` | SnapTrade signing key | required |
| `NS_MAX_ORDER_CAD` | Hard order cap (CAD) | 2000 |
| `NS_KILL_SWITCH` | Block all orders if `true` | unset |
| `AI_INTEGRATIONS_ANTHROPIC_BASE_URL` | Replit AI proxy base | required for AI |
| `AI_INTEGRATIONS_ANTHROPIC_API_KEY` | Anthropic key | required for AI |
| `AI_INTEGRATIONS_OPENAI_BASE_URL` | Replit OpenAI proxy | required for AI |
| `AI_INTEGRATIONS_OPENAI_API_KEY` | OpenAI key | required for AI |

## Branch Conventions

- Feature branches: `feat/<short-name>`
- Bug fixes: `fix/<short-name>`
- Claude-assisted work: `claude/<description>`
- CI/infra: `chore/<description>`
- Docs: `docs/<description>`

## Portfolio Context (TFSA)

Holdings: AMD, BB, BB.TO, ENB, ORCL, PANW, PLTR, TSM  
CAD/USD conversion rate used in signal scoring: **0.736**  
Base currency: CAD
