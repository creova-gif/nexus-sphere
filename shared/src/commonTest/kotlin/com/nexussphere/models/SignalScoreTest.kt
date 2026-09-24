package com.nexussphere.models

import com.nexussphere.domain.MarketSnapshot
import com.nexussphere.domain.Signal
import com.nexussphere.domain.SignalDirection
import com.nexussphere.domain.Symbol
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignalScoreTest {

    private fun snapshot(
        rsi14: Double = 50.0,
        macdLine: Double = 0.0,
        macdSignal: Double = 0.0,
        price: Double = 100.0,
        sma50: Double = 100.0,
        sma200: Double = 100.0,
        peRatio: Double? = 20.0,
        pbRatio: Double? = 2.0,
        revenueGrowthYoy: Double? = 0.12,
        mom1m: Double = 0.03,
        dcfFairValue: Double? = 130.0,
        marketCycleStage: Int = 1,
    ) = MarketSnapshot(
        symbol = Symbol("AMD"),
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
        asOf = Instant.fromEpochSeconds(0),
    )

    @Test
    fun strongBuyExceedsProfitGate() {
        val snap = snapshot(
            rsi14 = 28.0,           // oversold — full RSI points
            macdLine = 1.0,         // MACD bullish
            price = 100.0,
            sma50 = 95.0,           // price above both MAs
            sma200 = 90.0,
            marketCycleStage = 1,   // early expansion
            dcfFairValue = 150.0,   // 33% upside
            peRatio = 18.0,
            pbRatio = 2.0,
            revenueGrowthYoy = 0.20,
            mom1m = 0.07,
        )
        val sig = SignalScore.evaluate(snap)
        assertTrue(sig.score >= Signal.PROFIT_GATE, "Expected ≥${Signal.PROFIT_GATE}, got ${sig.score}")
        assertTrue(sig.profitGated)
        assertEquals(SignalDirection.BUY, sig.direction)
    }

    @Test
    fun weakSetupBelowProfitGate() {
        val snap = snapshot(
            rsi14 = 72.0,           // overbought
            macdLine = -0.5,        // MACD bearish
            price = 80.0,
            sma50 = 95.0,           // price below both MAs
            sma200 = 90.0,
            marketCycleStage = 4,   // contraction
            dcfFairValue = 75.0,    // overvalued
            peRatio = 60.0,         // expensive
            pbRatio = 8.0,
            revenueGrowthYoy = 0.02,
            mom1m = -0.05,
        )
        val sig = SignalScore.evaluate(snap)
        assertTrue(sig.score < Signal.PROFIT_GATE, "Expected <${Signal.PROFIT_GATE}, got ${sig.score}")
        assertFalse(sig.profitGated)
    }

    @Test
    fun scoreIsWithinBounds() {
        val sig = SignalScore.evaluate(snapshot())
        assertTrue(sig.score in 0..100)
    }

    @Test
    fun oversoldRsiMaximisesRsiComponent() {
        val withOversold  = SignalScore.evaluate(snapshot(rsi14 = 25.0))
        val withNeutral   = SignalScore.evaluate(snapshot(rsi14 = 50.0))
        assertTrue(withOversold.rsiScore > withNeutral.rsiScore)
    }
}
