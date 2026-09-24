package com.nexussphere.data.repository

import com.nexussphere.domain.Symbol
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DemoMarketDataRepositoryTest {

    private val repo = DemoMarketDataRepository()

    @Test
    fun allTfsaHoldingsHaveSnapshots() = runTest {
        val results = repo.getSnapshots(Symbol.TFSA_HOLDINGS)
        assertEquals(Symbol.TFSA_HOLDINGS.size, results.size)
        results.values.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun allSnapshotsHavePositivePrices() = runTest {
        val results = repo.getSnapshots(Symbol.TFSA_HOLDINGS)
        results.values.forEach { result ->
            val snap = result.getOrThrow()
            assertTrue(snap.price > 0.0, "${snap.symbol} has non-positive price")
        }
    }

    @Test
    fun rsiInValidRange() = runTest {
        val results = repo.getSnapshots(Symbol.TFSA_HOLDINGS)
        results.values.forEach { result ->
            val snap = result.getOrThrow()
            assertTrue(snap.rsi14 in 0.0..100.0, "${snap.symbol} RSI out of range: ${snap.rsi14}")
        }
    }

    @Test
    fun unknownSymbolReturnsFailure() = runTest {
        val result = repo.getSnapshot(Symbol("UNKNOWN"))
        assertTrue(result.isFailure)
    }

    @Test
    fun amdSnapshotHasExpectedPrice() = runTest {
        val snap = repo.getSnapshot(Symbol("AMD")).getOrThrow()
        assertEquals("AMD", snap.symbol.value)
        assertTrue(snap.price > 0.0)
    }

    @Test
    fun bbSnapshotHasNullPe() = runTest {
        val snap = repo.getSnapshot(Symbol("BB")).getOrThrow()
        assertEquals(null, snap.peRatio, "BB has no P/E in demo data")
    }

    @Test
    fun canadianSymbolPresentInSnapshots() = runTest {
        val snap = repo.getSnapshot(Symbol("BB.TO")).getOrThrow()
        assertTrue(snap.symbol.isCanadian)
        assertNotNull(snap.price)
    }
}
