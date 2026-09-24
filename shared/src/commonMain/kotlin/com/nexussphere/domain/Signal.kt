package com.nexussphere.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Direction of the trading signal. */
@Serializable
enum class SignalDirection { BUY, SELL, HOLD }

/**
 * Output of the Combined Signal Score engine.
 * Score range: 0–100. Auto-execution requires score ≥ [PROFIT_GATE].
 */
@Serializable
data class Signal(
    val symbol: Symbol,
    val score: Int,
    val direction: SignalDirection,
    val profitGated: Boolean,
    // Component scores (0-100 each)
    val rsiScore: Int,
    val macdScore: Int,
    val trendScore: Int,
    val stageScore: Int,
    val dcfScore: Int,
    val fundamentalsScore: Int,
    val momentumScore: Int,
    val generatedAt: Instant,
) {
    val isActionable: Boolean get() = profitGated && score >= PROFIT_GATE

    companion object {
        const val PROFIT_GATE = 65
    }
}

/** Raw market inputs consumed by the signal engine. */
@Serializable
data class MarketSnapshot(
    val symbol: Symbol,
    val price: Double,
    val rsi14: Double,          // 0–100
    val macdLine: Double,
    val macdSignal: Double,
    val sma50: Double,
    val sma200: Double,
    val peRatio: Double?,
    val pbRatio: Double?,
    val revenueGrowthYoy: Double?,
    val mom1m: Double,          // 1-month price momentum (fraction, e.g. 0.05 = +5%)
    val dcfFairValue: Double?,
    val marketCycleStage: Int,  // 1=early, 2=mid, 3=late, 4=contraction
    val asOf: Instant,
)
