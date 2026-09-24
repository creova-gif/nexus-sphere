package com.nexussphere.data.repository

import com.nexussphere.data.network.MarketDataApiClient
import com.nexussphere.domain.MarketSnapshot
import com.nexussphere.domain.Symbol
import com.nexussphere.models.TechnicalIndicators
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class MarketDataRepositoryImpl(
    private val apiClient: MarketDataApiClient,
    private val demo: MarketDataRepository = DemoMarketDataRepository(),
) : MarketDataRepository {

    override suspend fun getSnapshot(symbol: Symbol): Result<MarketSnapshot> =
        runCatching { fetchSnapshot(symbol) }
            .recoverCatching { demo.getSnapshot(symbol).getOrThrow() }

    override suspend fun getSnapshots(symbols: Set<Symbol>): Map<Symbol, Result<MarketSnapshot>> =
        symbols.associateWith { getSnapshot(it) }

    private suspend fun fetchSnapshot(symbol: Symbol): MarketSnapshot {
        val chartResp = apiClient.fetchChart(symbol.value)
        val result = chartResp.chart.result?.firstOrNull()
            ?: error("No chart data for ${symbol.value}")

        val closes = result.indicators?.quote?.firstOrNull()?.close
            ?.filterNotNull() ?: emptyList()

        val price = result.meta.regularMarketPrice
        val rsi14 = if (closes.size >= 15) TechnicalIndicators.rsi(closes) else 50.0
        val sma50 = TechnicalIndicators.sma(closes, 50)
        val sma200 = TechnicalIndicators.sma(closes, 200)
        val macdResult = TechnicalIndicators.macd(closes)

        // fetch fundamentals
        val quoteResp = runCatching { apiClient.fetchQuote(listOf(symbol.value)) }.getOrNull()
        val q = quoteResp?.quoteResponse?.result?.firstOrNull()

        val mom1m = if (closes.size >= 22) {
            val monthAgo = closes[closes.size - 22]
            if (monthAgo > 0.0) (price - monthAgo) / monthAgo else 0.0
        } else 0.0

        return MarketSnapshot(
            symbol = symbol,
            price = price,
            rsi14 = if (rsi14.isNaN()) 50.0 else rsi14,
            macdLine = macdResult?.macdLine ?: 0.0,
            macdSignal = macdResult?.signalLine ?: 0.0,
            sma50 = if (sma50.isNaN()) price else sma50,
            sma200 = if (sma200.isNaN()) price else sma200,
            peRatio = q?.trailingPE,
            pbRatio = q?.priceToBook,
            revenueGrowthYoy = q?.revenueGrowth,
            mom1m = mom1m,
            dcfFairValue = null,
            marketCycleStage = inferCycleStage(sma50, sma200, price),
            asOf = Clock.System.now(),
        )
    }

    private fun inferCycleStage(sma50: Double, sma200: Double, price: Double): Int = when {
        sma50.isNaN() || sma200.isNaN() -> 3
        price > sma50 && sma50 > sma200 -> 3   // late expansion
        price < sma50 && sma50 < sma200 -> 4   // contraction
        price > sma200 -> 2                     // mid
        else -> 1                               // early
    }
}
