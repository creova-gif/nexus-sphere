package com.nexussphere.data.repository

import com.nexussphere.domain.Signal
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoPortfolioRepositoryTest {

    private val repo = DemoPortfolioRepository()

    @Test
    fun allEightTfsaHoldingsPresent() = runTest {
        val result = repo.getPositions("", "")
        assertTrue(result.isSuccess)
        val symbols = result.getOrThrow().map { it.symbol.value }.toSet()
        val expected = setOf("AMD", "BB", "BB.TO", "ENB", "ORCL", "PANW", "PLTR", "TSM")
        assertEquals(expected, symbols)
    }

    @Test
    fun allPositionsHavePositiveUnits() = runTest {
        val positions = repo.getPositions("", "").getOrThrow()
        positions.forEach { assertTrue(it.units > 0.0, "${it.symbol} has non-positive units") }
    }

    @Test
    fun portfolioTotalEquityIsPositive() = runTest {
        val portfolio = repo.getPortfolio("", "").getOrThrow()
        assertTrue(portfolio.totalEquityCad.isPositive())
    }

    @Test
    fun portfolioAccountNameContainsDemo() = runTest {
        val portfolio = repo.getPortfolio("", "").getOrThrow()
        assertTrue(portfolio.accountName.contains("Demo", ignoreCase = true))
    }

    @Test
    fun positionsWithGainHavePositivePnl() = runTest {
        val positions = repo.getPositions("", "").getOrThrow()
        // AMD, ENB, ORCL, PANW, PLTR, TSM are all priced above avg cost in demo data
        val gainers = positions.filter { it.isProfit }
        assertTrue(gainers.isNotEmpty(), "Expected some positions with unrealised gains")
        gainers.forEach { assertTrue(it.unrealisedPnlCad.isPositive()) }
    }

    @Test
    fun bbPositionIsUnderwater() = runTest {
        val positions = repo.getPositions("", "").getOrThrow()
        val bb = positions.first { it.symbol.value == "BB" }
        assertTrue(bb.unrealisedPnlCad.isNegative(), "BB should show unrealised loss in demo data")
    }
}
