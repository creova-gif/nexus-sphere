package com.nexussphere.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KellyCriterionTest {

    @Test
    fun positiveEdgeReturnsFraction() {
        val f = KellyCriterion.fraction(winRate = 0.6, avgWin = 0.05, avgLoss = 0.03)
        assertTrue(f > 0.0, "Positive edge should yield positive fraction")
    }

    @Test
    fun zeroEdgeClampsToZero() {
        // winRate * avgWin == (1-winRate) * avgLoss → Kelly = 0
        val f = KellyCriterion.fraction(winRate = 0.375, avgWin = 0.04, avgLoss = 0.025)
        // result may be very small negative due to float; must clamp ≥ 0
        assertTrue(f >= 0.0)
    }

    @Test
    fun negativeEdgeClampsToZero() {
        val f = KellyCriterion.fraction(winRate = 0.3, avgWin = 0.02, avgLoss = 0.05)
        assertEquals(0.0, f)
    }

    @Test
    fun halfKellyReducesFraction() {
        val full = KellyCriterion.fraction(0.6, 0.05, 0.03, riskAversion = 1.0)
        val half = KellyCriterion.fraction(0.6, 0.05, 0.03, riskAversion = 0.5)
        assertTrue(half < full)
    }

    @Test
    fun zeroAvgLossReturnsZero() {
        val f = KellyCriterion.fraction(0.7, 0.05, avgLoss = 0.0)
        assertEquals(0.0, f)
    }

    @Test
    fun positionSizeScalesWithEquity() {
        val size = KellyCriterion.positionSizeCad(
            portfolioEquityCad = 10_000.0,
            winRate = 0.6,
            avgWin = 0.05,
            avgLoss = 0.03,
        )
        assertTrue(size > 0.0 && size < 10_000.0)
    }
}
