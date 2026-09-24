package com.nexussphere.models

import kotlin.math.max

/**
 * Kelly Criterion position sizing.
 *
 * Full Kelly is rarely used in practice — [riskAversion] of 0.5 (half Kelly)
 * is the default to reduce variance at the cost of slightly lower growth.
 *
 * Must be recomputed on each auto-execution; never cache the result.
 */
object KellyCriterion {

    /**
     * Returns the fraction of capital to deploy (0.0–1.0, clamped to non-negative).
     *
     * @param winRate   Probability of a winning trade (0.0–1.0)
     * @param avgWin    Average gain on a winning trade (positive fraction, e.g. 0.05 = 5%)
     * @param avgLoss   Average loss on a losing trade (positive fraction, e.g. 0.03 = 3%)
     * @param riskAversion Multiplier applied to full Kelly (0.5 = half Kelly)
     */
    fun fraction(
        winRate: Double,
        avgWin: Double,
        avgLoss: Double,
        riskAversion: Double = 0.5,
    ): Double {
        require(winRate in 0.0..1.0)    { "winRate must be in [0, 1]" }
        require(avgWin >= 0.0)          { "avgWin must be non-negative" }
        require(avgLoss >= 0.0)         { "avgLoss must be non-negative" }
        require(riskAversion in 0.0..1.0) { "riskAversion must be in [0, 1]" }

        if (avgLoss == 0.0 || avgWin == 0.0) return 0.0

        val b = avgWin / avgLoss          // win/loss ratio
        val p = winRate
        val q = 1.0 - winRate

        val fullKelly = (b * p - q) / b
        return max(0.0, fullKelly * riskAversion)
    }

    /**
     * Returns the recommended position size in CAD given portfolio equity.
     */
    fun positionSizeCad(
        portfolioEquityCad: Double,
        winRate: Double,
        avgWin: Double,
        avgLoss: Double,
        riskAversion: Double = 0.5,
    ): Double {
        val f = fraction(winRate, avgWin, avgLoss, riskAversion)
        return portfolioEquityCad * f
    }
}
