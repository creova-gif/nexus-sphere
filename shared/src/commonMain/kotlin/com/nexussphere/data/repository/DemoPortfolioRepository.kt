package com.nexussphere.data.repository

import com.nexussphere.domain.*
import kotlinx.datetime.Clock

/**
 * Offline demo data for the TFSA portfolio.
 * Used when broker is not connected or server is unreachable.
 * Prices are approximate and for display only — never used for execution.
 */
class DemoPortfolioRepository : PortfolioRepository {

    override suspend fun getPositions(userId: String, userSecret: String): Result<List<Position>> =
        Result.success(demoPositions())

    override suspend fun getPortfolio(userId: String, userSecret: String): Result<Portfolio> {
        val positions = demoPositions()
        return Result.success(
            Portfolio.fromPositions(
                accountId   = "DEMO-TFSA-001",
                accountName = "TFSA (Demo)",
                positions   = positions,
                cashCad     = 842.50,
                asOf        = Clock.System.now(),
            )
        )
    }

    private fun demoPositions(): List<Position> = listOf(
        Position.of(Symbol("AMD"),    units = 10.0,  avgCostCad = 142.80, currentPriceCad = 181.20),
        Position.of(Symbol("BB"),     units = 100.0, avgCostCad = 9.45,   currentPriceCad = 7.10),
        Position.of(Symbol("BB.TO"),  units = 50.0,  avgCostCad = 11.20,  currentPriceCad = 9.05,  currency = Currency.CAD),
        Position.of(Symbol("ENB"),    units = 30.0,  avgCostCad = 51.30,  currentPriceCad = 58.40),
        Position.of(Symbol("ORCL"),   units = 5.0,   avgCostCad = 155.00, currentPriceCad = 202.60),
        Position.of(Symbol("PANW"),   units = 3.0,   avgCostCad = 310.00, currentPriceCad = 382.90),
        Position.of(Symbol("PLTR"),   units = 20.0,  avgCostCad = 78.50,  currentPriceCad = 122.40),
        Position.of(Symbol("TSM"),    units = 8.0,   avgCostCad = 190.00, currentPriceCad = 251.80),
    )
}
