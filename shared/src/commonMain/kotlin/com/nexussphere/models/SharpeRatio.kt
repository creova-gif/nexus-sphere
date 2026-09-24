package com.nexussphere.models

import kotlin.math.sqrt

/**
 * Sharpe Ratio calculation for strategy performance evaluation.
 * A Sharpe ≥ 1.0 is required before a strategy may be promoted to LIVE state.
 */
object SharpeRatio {

    /**
     * Annualised Sharpe Ratio.
     *
     * @param returns        Sequence of periodic returns (fractions, e.g. 0.01 = 1%)
     * @param riskFreeRate   Periodic risk-free rate (same frequency as returns)
     * @param periodsPerYear Periods per year (252 for daily, 52 for weekly, 12 for monthly)
     * @return Sharpe ratio, or 0.0 if returns has fewer than 2 observations
     */
    fun annualised(
        returns: List<Double>,
        riskFreeRate: Double = 0.0,
        periodsPerYear: Int = 252,
    ): Double {
        if (returns.size < 2) return 0.0

        val excess = returns.map { it - riskFreeRate }
        val mean = excess.average()
        val variance = excess.sumOf { (it - mean) * (it - mean) } / (excess.size - 1)
        val stdDev = sqrt(variance)

        if (stdDev == 0.0) return 0.0
        return (mean / stdDev) * sqrt(periodsPerYear.toDouble())
    }
}
