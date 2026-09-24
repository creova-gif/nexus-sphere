package com.nexussphere.data.repository

import com.nexussphere.domain.MarketSnapshot
import com.nexussphere.domain.Symbol

interface MarketDataRepository {
    suspend fun getSnapshot(symbol: Symbol): Result<MarketSnapshot>
    suspend fun getSnapshots(symbols: Set<Symbol>): Map<Symbol, Result<MarketSnapshot>>
}
