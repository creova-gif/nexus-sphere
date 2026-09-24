package com.nexussphere.models

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechnicalIndicatorsTest {

    private val prices20 = listOf(
        100.0, 102.0, 101.5, 103.0, 105.0,
        104.5, 106.0, 107.0, 106.5, 108.0,
        109.0, 107.5, 110.0, 111.0, 110.5,
        112.0, 113.0, 112.5, 114.0, 115.0,
    )

    @Test
    fun smaOnExactPeriod() {
        val result = TechnicalIndicators.sma(prices20.take(5), 5)
        assertEquals(102.3, result, 0.01)
    }

    @Test
    fun smaTooShortReturnsNaN() {
        assertTrue(TechnicalIndicators.sma(listOf(1.0, 2.0), 10).isNaN())
    }

    @Test
    fun emaDecaysOlderPrices() {
        val prices = List(15) { (it + 1).toDouble() }
        val ema = TechnicalIndicators.ema(prices, 10)
        assertFalse(ema.isNaN())
        assertTrue(ema > prices.average(), "EMA should weight recent prices higher in uptrend")
    }

    @Test
    fun rsiOversoldBelowThirty() {
        val declining = (100 downTo 80).map { it.toDouble() }
        val rsi = TechnicalIndicators.rsi(declining, 14)
        assertFalse(rsi.isNaN())
        assertTrue(rsi < 30.0, "Declining prices should produce RSI < 30, got $rsi")
    }

    @Test
    fun rsiOverboughtAboveSeventy() {
        val rising = (80..102).map { it.toDouble() }
        val rsi = TechnicalIndicators.rsi(rising, 14)
        assertFalse(rsi.isNaN())
        assertTrue(rsi > 70.0, "Rising prices should produce RSI > 70, got $rsi")
    }

    @Test
    fun rsiTooShortReturnsNaN() {
        assertTrue(TechnicalIndicators.rsi(listOf(1.0, 2.0, 3.0), 14).isNaN())
    }

    @Test
    fun macdPositiveInUptrend() {
        val rising = (50..90).map { it.toDouble() }
        val result = TechnicalIndicators.macd(rising)
        assertTrue(result != null && result.macdLine > 0.0, "MACD line should be positive in uptrend")
    }

    @Test
    fun macdNullWhenTooShort() {
        val result = TechnicalIndicators.macd(listOf(1.0, 2.0, 3.0))
        assertEquals(null, result)
    }
}
