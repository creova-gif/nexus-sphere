# NexusSphere — Global Macro Terminal v5

## Overview

A two-page financial intelligence application served via Flask. Features live Wealthsimple trading via SnapTrade, auto-strategy engine, and personal investment advice.

## Routes

- `/` — NexusSphere Global Macro Terminal: full-featured financial terminal with market cycle engine, portfolio tracker, news & sentiment, risk analytics, tax/performance, goals, sandbox, earnings, quant models, prediction market bot, trading engine, stock screener (Finviz-style TSX + US), and an embedded Big Mac Index tab.
- `/bigmac` — Standalone Big Mac Index Currency Valuation Explorer with interactive world map, bar chart view, trend comparisons, and country rankings.
- `/snap-callback` — SnapTrade OAuth callback (redirects back to the app after broker login).

## Architecture

- **Backend**: Flask (Python), `main.py` on port 5000
- **Frontend**: Two standalone HTML files, each self-contained with embedded CSS, JS, Chart.js, D3.js, TopoJSON
  - `static_nexussphere.html` — NexusSphere terminal (~23,000 lines)
  - `static_bigmac.html` — Big Mac Index explorer

## AI Integration (OpenAI + Claude via Replit)

Both OpenAI and Anthropic (Claude) are connected via Replit AI Integrations — no user API keys required, billed to Replit credits. Models confirmed working: `claude-sonnet-4-5` (Anthropic) and `gpt-4o` (OpenAI).

### Backend AI proxy routes (all in `main.py`):
| Route | Method | Purpose |
|---|---|---|
| `/api/ai/claude` | POST | Proxy to Claude — accepts `{messages, max_tokens, system}`; returns Anthropic-format `{content:[{text}]}` |
| `/api/ai/openai` | POST | Proxy to GPT-4o — same body format, same response shape |
| `/api/ai/news-summary` | POST | Summarize headlines for a symbol; returns `{summary, sentiment, confidence}` |

All three AI routes require `Authorization: Bearer $NEXUS_API_TOKEN` and share a per-user rate limit. All frontend calls route through these proxies. The following functions call `/api/ai/claude`:
- `pmCallClaudeForProbability()` — prediction market probability engine
- PM research pipeline — market research brief generator
- `aiChatWithClaude()` — trade journal AI coach
- `aiReviewTrade()` — per-trade AI review

## SnapTrade Integration (Wealthsimple Live Trading)

Requires two environment secrets: `SNAPTRADE_CLIENT_ID` and `SNAPTRADE_CONSUMER_KEY` (from app.snaptrade.com).

### Backend proxy routes (all in `main.py`):
| Route | Method | Purpose |
|---|---|---|
| `/api/snap/status` | GET | Check if credentials are configured |
| `/api/snap/register` | POST | Register a new SnapTrade user |
| `/api/snap/connect` | POST | Get Wealthsimple OAuth URL |
| `/api/snap/accounts` | GET | List user's brokerage accounts |
| `/api/snap/positions` | GET | Get all holdings |
| `/api/snap/balances` | GET | Get account balances |
| `/api/snap/activities` | GET | Get transaction history |
| `/api/snap/symbols` | GET | Symbol search (exchanges: NASDAQ/NYSE/TSX/ARCA) |
| `/api/snap/quote` | GET | Real-time quotes |
| `/api/snap/order/impact` | POST | Preview order (returns trade_id + estimated cost). Requires bearer auth. |
| `/api/snap/order/place` | POST | Confirm order using trade_id. Requires bearer auth. |
| `/api/snap/order/force` | POST | Disabled. Returns 403 and does not call the brokerage. |
| `/api/snap/order/cancel` | POST | Cancel open order. Requires bearer auth. |
| `/api/snap/orders` | GET | List orders by state. Requires bearer auth. |

### Trading and AI guardrails enforced in the server:
- `Authorization: Bearer $NEXUS_API_TOKEN` required on `/api/ai/*` and on order preview, place, cancel, force, and order list. Requests fail closed when the token is unset.
- Per-authenticated-user rate limit on `/api/ai/*` (`AI_RATE_LIMIT` per `AI_RATE_WINDOW_SECONDS`, default 30 per 60 seconds).
- `/api/snap/order/force` is disabled and cannot skip preview.
- Preview `tradeId` is required to place an order.
- Equity preview actions are BUY and SELL only.
- AI `max_tokens` is clamped (`AI_MAX_TOKENS_CEILING`, default 2048).

The $2,000 CAD per-order cap and 30% concentration cap are not enforced in code.

## Features

- **Trading Engine tab**: 9 sub-tabs — Dashboard, Intraday, Live News, Signals, Adv Models, AI Assistant, Execution (order ticket + DCA), L2 Order Book, Trade Blotter, Journal, Canadian
  - **L2 Order Book**: 10-level bid/ask depth ladder with proportional depth bars, center spread/imbalance panel, 22-row time & sales tape, real bid/ask from SnapTrade quote, auto-refresh every 2s, 12-ticker selector
  - **Trade Blotter**: Full order history table (9 columns), status/symbol filters, summary stats bar, CSV export, auto-refresh every 30s
- **AI Assistant tab**: Personal TFSA advice for AMD/TSM/ORCL/PANW/PLTR/ENB holdings + AI chat
- **Portfolio Tracker**: Deep Dive sub-nav (Overview | ACB & Tax | Rebalancing | P&L Deep Dive | Fundamentals); ACB tracking, TFSA room tracker, capital gains simulator, drift-based rebalancing engine, benchmark P&L chart; Fundamentals tab with P/E comparison bars, analyst target bar chart, quality score cards (profitability/growth/safety), dividend income analysis for all 7 holdings
- **Market Terminal**: Stage classifier, macro tables, mixed Chart.js charts with volume + MA overlay
- **Stock Screener**: Comprehensive Finviz-replacement. 75+ stocks (TSX/TSX-V/US/INTL). 24 filters (market, sector, MCap, P/E, RSI, dividend, beta, SMA, industry, country, index, price range, avg vol, theme, earnings date, float). 5 colsets (Overview/Technical/Financial/Fundamentals/Ownership). 11 views (Table, Heatmap, Sectors, Insider, ETF, Snapshot, News, Calendar, Stats, Managers, Funds). 19 presets. INDEX_MEMBERS (S&P500/NASDAQ100/TSX60/Dow30) and THEME_MEMBERS (AI/cloud/cyber/chips/EV/biotech/gold/dividend) lookup tables.
- **Path of Price — Charts**: Interactive price chart with Bollinger Bands, SMA 50/200, VWAP; RSI panel, MACD panel; 11 tickers, 5 timeframes; real-time signal bar (RSI/MACD/BB%/VWAP)
- **News & Sentiment**: 25+ stories; CANADA, TECH, MY HOLDINGS filters; List + Heatmap views; impact badges; clickable ticker links → Charts
- **Quant Models**: 16 models — DCF, CAPM, Black-Scholes, Monte Carlo, Factor (FF5), Kelly Criterion, Regime Classifier, Backtester, Technicals, Risk Metrics, TFSA Signals, DDM, Graham Number, Altman Z-Score, Piotroski F-Score, Efficient Frontier
- **Tools**: 6 calculators — Compound Interest, DCA (Dollar-Cost Averaging), RRSP room calculator, Options P&L (payoff at expiry), Inflation-Adjusted Real Return, Bond Yield (YTM + duration)
- **Goals & Planning**: Goal tracker + CPP/OAS estimator, TFSA vs RRSP vs FHSA optimizer, cash flow planner
- **Tax**: ACB tracker, superficial loss detector, T5008/T5 slip estimator, FHSA room tracker
- **Sandbox**: Trade simulator, macro scenario builder (rate/oil/USD-CAD shocks), side-by-side portfolio compare
- **Global**: CMD+K global search, keyboard navigation, notification center, VaR/CVaR with 6 stress scenarios
- **Global Macro Map**: New full-page D3 choropleth with 7 data layers — GDP Growth, Inflation, Interest Rates, Equity YTD, FX vs USD, Market Hours (real-time open/closed/pre-post per exchange), and Trade Flows (14 animated arcs between G20 nations). 30-country dataset. Country click → sliding detail panel with macro stats and exchange info.
- **Big Mac Index**: D3.js world map + bar chart, 26 years of data, animated year slider
- **Terms of Service & Privacy Policy**: Accessible via masthead strip and status bar footer

## Design — Dark Ivory Theme

Luxury wealth management terminal (Bloomberg × private banking aesthetic):
- **Font stack**: Playfair Display serif (logo/headings), Barlow Condensed (display), Barlow (body), JetBrains Mono (data)
- **Surfaces**: `#111111` obsidian → `#181818` → `#1E1E1E` → `#252525` → `#2E2E2E` → `#383838`
- **Text**: `#F2F0EB` pearl primary → `#A89F8C` warm secondary → `#665E50` tertiary
- **Buy / positive**: `#D4AF37` soft gold with `0 0 14px rgba(212,175,55,.28)` glow
- **Sell / negative**: `#E11D48` rose with `0 0 14px rgba(225,29,72,.28)` glow
- **AI accent**: `#C8A84B` amber with `0 0 24px rgba(200,168,75,.14)` glow (always alive, never distracting)
- **Borders**: rgba(255,255,255,.04/.08/.14) luminous hairlines on dark
- **Masthead strip**: `#0A0A0A` pure black with `rgba(212,175,55,.15)` gold bottom border
- **Topbar**: `#181818` with gold bottom separator, Playfair Display logo, gold "SPHERE" em
- **Tab active**: gold underline (not white)
- **Status bar**: pure black floor with gold data values
- **Loader**: obsidian field, Playfair Display serif logo, gold progress bar
- **Modals**: dark surface with gold border hairline, soft drop shadow
- **Legal modals**: gold "CLOSE" button, gold section headers
- BigMac tab: starts in dark mode by default

## Running

Workflow `Start application` runs `python main.py` on port 5000.

## Dependencies

Managed by uv/pyproject.toml. Key packages: `flask`, `requests`, `snaptrade-python-sdk`.
