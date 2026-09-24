package com.nexussphere.domain

import kotlinx.serialization.Serializable

/**
 * A single holding in the TFSA portfolio.
 * All monetary values are in CAD (converted from USD where applicable).
 */
@Serializable
data class Position(
    val symbol: Symbol,
    val units: Double,
    val averageCostCad: Money,
    val currentPriceCad: Money,
    val marketValueCad: Money,
    val unrealisedPnlCad: Money,
    val unrealisedPnlPct: Double,
    val currency: Currency,
    val signal: Signal? = null,        // populated after signal engine runs
) {
    val isProfit: Boolean get() = unrealisedPnlCad.isPositive()

    companion object {
        /** Build a position from raw price data, computing derived fields. */
        fun of(
            symbol: Symbol,
            units: Double,
            avgCostCad: Double,
            currentPriceCad: Double,
            currency: Currency = Currency.CAD,
        ): Position {
            val cost   = Money.ofCad(avgCostCad)
            val price  = Money.ofCad(currentPriceCad)
            val market = price * units
            val bookValue = cost * units
            val pnl    = market - bookValue
            val pnlPct = if (avgCostCad > 0) (currentPriceCad - avgCostCad) / avgCostCad else 0.0
            return Position(
                symbol             = symbol,
                units              = units,
                averageCostCad     = cost,
                currentPriceCad    = price,
                marketValueCad     = market,
                unrealisedPnlCad   = pnl,
                unrealisedPnlPct   = pnlPct,
                currency           = currency,
            )
        }
    }
}
