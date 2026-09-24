package com.nexussphere.models

import com.nexussphere.domain.MarketSnapshot
import com.nexussphere.domain.Signal
import com.nexussphere.domain.SignalDirection
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

/**
 * Combined Signal Score engine (0–100).
 * Weights are fixed; any adjustment requires a documented back-test comparison per CLAUDE.md.
 *
 * Component weights:
 *   RSI          20
 *   MACD         15
 *   Trend        15
 *   Stage        10
 *   DCF          15
 *   Fundamentals 15
 *   Momentum     10
 *   ─────────────
 *   Total       100
 */
object SignalScore {

    private const val WEIGHT_RSI   = 20
    private const val WEIGHT_MACD  = 15
    private const val WEIGHT_TREND = 15
    private const val WEIGHT_STAGE = 10
    private const val WEIGHT_DCF   = 15
    private const val WEIGHT_FUNDS = 15
    private const val WEIGHT_MOM   = 10

    fun evaluate(snap: MarketSnapshot, clock: Clock = Clock.System): Signal {
        val rsiScore   = scoreRsi(snap.rsi14)
        val macdScore  = if (snap.macdLine > snap.macdSignal) WEIGHT_MACD else 0
        val trendScore = scoreTrend(snap.price, snap.sma50, snap.sma200)
        val stageScore = scoreStage(snap.marketCycleStage)
        val dcfScore   = scoreDcf(snap.price, snap.dcfFairValue)
        val fundsScore = scoreFundamentals(snap.peRatio, snap.pbRatio, snap.revenueGrowthYoy)
        val momScore   = scoreMomentum(snap.mom1m)

        val total = (rsiScore + macdScore + trendScore + stageScore +
                     dcfScore + fundsScore + momScore)
            .coerceIn(0, 100)

        val direction = when {
            total >= Signal.PROFIT_GATE -> SignalDirection.BUY
            total <= 35                 -> SignalDirection.SELL
            else                        -> SignalDirection.HOLD
        }

        return Signal(
            symbol          = snap.symbol,
            score           = total,
            direction       = direction,
            profitGated     = total >= Signal.PROFIT_GATE,
            rsiScore        = rsiScore,
            macdScore       = macdScore,
            trendScore      = trendScore,
            stageScore      = stageScore,
            dcfScore        = dcfScore,
            fundamentalsScore = fundsScore,
            momentumScore   = momScore,
            generatedAt     = clock.now(),
        )
    }

    // ── Component scorers ────────────────────────────────────────────────

    private fun scoreRsi(rsi: Double): Int = when {
        rsi < 30  -> WEIGHT_RSI           // oversold — full points
        rsi < 45  -> (WEIGHT_RSI * 0.75).roundToInt()
        rsi < 55  -> (WEIGHT_RSI * 0.50).roundToInt()
        rsi < 70  -> (WEIGHT_RSI * 0.25).roundToInt()
        else      -> 0                    // overbought
    }

    private fun scoreTrend(price: Double, sma50: Double, sma200: Double): Int = when {
        price > sma50 && price > sma200 && sma50 > sma200 -> WEIGHT_TREND         // full bull
        price > sma50 && price > sma200                   -> (WEIGHT_TREND * 0.5).roundToInt()
        price > sma50 || price > sma200                   -> (WEIGHT_TREND * 0.25).roundToInt()
        else                                               -> 0
    }

    private fun scoreStage(stage: Int): Int = when (stage) {
        1    -> WEIGHT_STAGE           // early expansion — best entry
        2    -> (WEIGHT_STAGE * 0.7).roundToInt()
        3    -> (WEIGHT_STAGE * 0.3).roundToInt()
        else -> 0                      // contraction
    }

    private fun scoreDcf(price: Double, fairValue: Double?): Int {
        if (fairValue == null || fairValue <= 0.0) return 0
        val upside = (fairValue - price) / fairValue
        return when {
            upside >= 0.30 -> WEIGHT_DCF
            upside >= 0.15 -> (WEIGHT_DCF * 0.6).roundToInt()
            upside >= 0.05 -> (WEIGHT_DCF * 0.3).roundToInt()
            else           -> 0
        }
    }

    private fun scoreFundamentals(pe: Double?, pb: Double?, revGrowth: Double?): Int {
        var score = 0
        if (pe != null && pe in 0.0..25.0)         score += (WEIGHT_FUNDS * 0.4).roundToInt()
        if (pb != null && pb in 0.0..3.0)           score += (WEIGHT_FUNDS * 0.3).roundToInt()
        if (revGrowth != null && revGrowth >= 0.10) score += (WEIGHT_FUNDS * 0.3).roundToInt()
        return score.coerceAtMost(WEIGHT_FUNDS)
    }

    private fun scoreMomentum(mom1m: Double): Int = when {
        mom1m >= 0.05 -> WEIGHT_MOM
        mom1m >= 0.02 -> (WEIGHT_MOM * 0.5).roundToInt()
        mom1m >= 0.0  -> (WEIGHT_MOM * 0.2).roundToInt()
        else          -> 0
    }
}
