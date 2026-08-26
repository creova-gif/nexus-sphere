# NexusSphere — Global Macro Terminal

**A personal financial intelligence terminal: live portfolio tracking via real brokerage integration, a market cycle engine, quantitative models, a prediction-market bot, a TSX+US stock screener, and a standalone Big Mac Index currency explorer.**

[![Status](https://img.shields.io/badge/status-active_development-yellow)]()
[![License](https://img.shields.io/badge/license-proprietary-red)]()

> **Privacy note:** this repository previously had real personal portfolio data (holdings, cost basis, account balances) committed in its history. That data has been fully purged from every branch's history — verified via a full `git log --all` scan post-purge, zero remaining traces. `.gitignore` now blocks the specific files that carried it. Everything in this repo uses clearly labeled demo/illustrative data only.

## Overview
A two-page Flask application: a financial intelligence terminal and a standalone currency-valuation explorer.

## Key Capabilities
- Live brokerage integration (Wealthsimple via SnapTrade API)
- Market-cycle classification engine, quantitative momentum models
- Risk analytics (Sharpe ratio, VaR, drawdown)
- Prediction-market edge-detection bot, earnings calendar, tax/performance tracking
- TSX + US stock screener (Finviz-style)
- Standalone Big Mac Index currency-valuation explorer (world map, bar charts, trend comparisons, country rankings)

## Architecture
Python/Flask, `uv` for dependency management.

## Testing
`uv sync` + pytest per CI. This repo has not had a deep audit in this engagement — verify actual test coverage directly (see akili-core's README for an example of a repo where "has test files" turned out not to mean "has real assertions") rather than assuming.

## Security
Real, verified history-purge already performed for previously-committed personal financial data. Maintain this discipline — never commit real account data, even for local testing.

## Project Status
Active development, personal-use tool.

## Contributing
Personal, proprietary project.

## License
Proprietary — All Rights Reserved.

## Author / Organization
Built by [Justin Mafie](https://github.com/creova-gif).
