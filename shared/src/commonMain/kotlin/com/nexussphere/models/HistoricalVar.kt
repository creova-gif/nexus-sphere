package com.nexussphere.models

/**
 * Historical Value-at-Risk (VaR) at a given confidence level.
 * Uses the historical simulation method (no distributional assumptions).
 */
object HistoricalVar {

    /**
     * Returns the VaR as a negative fraction representing the loss threshold.
     *
     * Example: VaR(0.95) = -0.02 means there is a 5% chance of losing
     * more than 2% in a single period.
     *
     * @param returns         Historical periodic returns
     * @param confidenceLevel e.g. 0.95 for 95% VaR
     * @return VaR value (negative = loss), or 0.0 if returns is empty
     */
    fun calculate(
        returns: List<Double>,
        confidenceLevel: Double = 0.95,
    ): Double {
        require(confidenceLevel in 0.0..1.0) { "confidenceLevel must be in [0, 1]" }
        if (returns.isEmpty()) return 0.0

        val sorted = returns.sorted()
        val idx = ((1.0 - confidenceLevel) * sorted.size).toInt()
            .coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
}
