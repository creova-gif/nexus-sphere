package com.nexussphere.data.repository

import com.nexussphere.domain.MarketSnapshot
import com.nexussphere.domain.Symbol
import kotlinx.datetime.Clock

class DemoMarketDataRepository : MarketDataRepository {

    private fun snap(
        symbol: Symbol,
        price: Double,
        rsi14: Double,
        macdLine: Double,
        macdSignal: Double,
        sma50: Double,
        sma200: Double,
        peRatio: Double?,
        pbRatio: Double?,
        revenueGrowthYoy: Double?,
        mom1m: Double,
        dcfFairValue: Double?,
        marketCycleStage: Int,
    ) = MarketSnapshot(
        symbol = symbol,
        price = price,
        rsi14 = rsi14,
        macdLine = macdLine,
        macdSignal = macdSignal,
        sma50 = sma50,
        sma200 = sma200,
        peRatio = peRatio,
        pbRatio = pbRatio,
        revenueGrowthYoy = revenueGrowthYoy,
        mom1m = mom1m,
        dcfFairValue = dcfFairValue,
        marketCycleStage = marketCycleStage,
        asOf = Clock.System.now(),
    )

    private val snapshots: Map<Symbol, MarketSnapshot> = mapOf(
        Symbol("AMD")   to snap(Symbol("AMD"),   164.50, 58.2, 1.85, 0.92, 158.40, 142.10, 38.4, 3.8, 0.22, 0.04, 175.0, 3),
        Symbol("BB")    to snap(Symbol("BB"),      3.10, 38.7, -0.12, 0.05, 3.20, 3.90, null, 1.1, -0.08, -0.06, 2.80, 2),
        Symbol("BB.TO") to snap(Symbol("BB.TO"),   4.25, 40.1, -0.09, 0.03, 4.40, 5.20, null, 1.2, -0.07, -0.05, 3.90, 2),
        Symbol("ENB")   to snap(Symbol("ENB"),    48.20, 52.4, 0.18, 0.10, 47.80, 46.20, 18.2, 2.0, 0.05, 0.02, 52.0, 3),
        Symbol("ORCL")  to snap(Symbol("ORCL"),  140.80, 62.1, 2.40, 1.20, 138.20, 120.50, 31.5, 12.4, 0.18, 0.07, 155.0, 3),
        Symbol("PANW")  to snap(Symbol("PANW"),  318.40, 65.8, 3.10, 1.80, 312.50, 278.00, 52.0, 15.2, 0.24, 0.09, 350.0, 4),
        Symbol("PLTR")  to snap(Symbol("PLTR"),   23.50, 55.3, 0.85, 0.42, 22.80, 18.40, 78.0, 11.0, 0.20, 0.06, 28.0, 3),
        Symbol("TSM")   to snap(Symbol("TSM"),   145.60, 60.5, 1.90, 0.95, 142.30, 128.60, 22.8, 5.6, 0.26, 0.05, 165.0, 3),
    )

    override suspend fun getSnapshot(symbol: Symbol): Result<MarketSnapshot> {
        return snapshots[symbol]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("No demo snapshot for $symbol"))
    }

    override suspend fun getSnapshots(symbols: Set<Symbol>): Map<Symbol, Result<MarketSnapshot>> =
        symbols.associateWith { getSnapshot(it) }
}
