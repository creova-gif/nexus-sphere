package com.nexussphere.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Portfolio(
    val accountId: String,
    val accountName: String,
    val positions: List<Position>,
    val totalMarketValueCad: Money,
    val totalBookValueCad: Money,
    val totalUnrealisedPnlCad: Money,
    val totalUnrealisedPnlPct: Double,
    val cashCad: Money,
    val asOf: Instant,
) {
    val totalEquityCad: Money get() = totalMarketValueCad + cashCad
    val isOverallProfit: Boolean get() = totalUnrealisedPnlCad.isPositive()

    companion object {
        fun fromPositions(
            accountId: String,
            accountName: String,
            positions: List<Position>,
            cashCad: Double = 0.0,
            asOf: Instant,
        ): Portfolio {
            val totalMarket = positions.fold(Money.zeroCad()) { acc, p -> acc + p.marketValueCad }
            val totalBook   = positions.fold(Money.zeroCad()) { acc, p -> acc + p.averageCostCad * p.units }
            val totalPnl    = totalMarket - totalBook
            val pnlPct      = if (totalBook.isPositive()) totalPnl.toDouble() / totalBook.toDouble() else 0.0
            return Portfolio(
                accountId              = accountId,
                accountName            = accountName,
                positions              = positions,
                totalMarketValueCad    = totalMarket,
                totalBookValueCad      = totalBook,
                totalUnrealisedPnlCad  = totalPnl,
                totalUnrealisedPnlPct  = pnlPct,
                cashCad                = Money.ofCad(cashCad),
                asOf                   = asOf,
            )
        }
    }
}
