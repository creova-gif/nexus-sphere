package com.nexussphere.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharpeRatioTest {

    @Test
    fun positiveReturnsYieldPositiveSharpe() {
        val returns = List(252) { 0.001 }  // 0.1% daily
        val sharpe = SharpeRatio.annualised(returns)
        assertTrue(sharpe > 0.0)
    }

    @Test
    fun negativeReturnsYieldNegativeSharpe() {
        val returns = List(252) { -0.001 }
        val sharpe = SharpeRatio.annualised(returns)
        assertTrue(sharpe < 0.0)
    }

    @Test
    fun singleObservationReturnsZero() {
        val sharpe = SharpeRatio.annualised(listOf(0.01))
        assertEquals(0.0, sharpe)
    }

    @Test
    fun emptyReturnsYieldZero() {
        assertEquals(0.0, SharpeRatio.annualised(emptyList()))
    }

    @Test
    fun higherVolatilityLowersSharpe() {
        val lowVol  = List(252) { 0.001 }
        val highVol = List(252) { if (it % 2 == 0) 0.01 else -0.008 }
        assertTrue(SharpeRatio.annualised(lowVol) > SharpeRatio.annualised(highVol))
    }
}
