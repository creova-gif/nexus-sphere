package com.nexussphere.data.repository

import com.nexussphere.domain.Portfolio
import com.nexussphere.domain.Position

interface PortfolioRepository {
    /** Fetch live holdings from the broker via Flask/SnapTrade. */
    suspend fun getPositions(userId: String, userSecret: String): Result<List<Position>>

    /** Fetch complete portfolio with totals. */
    suspend fun getPortfolio(userId: String, userSecret: String): Result<Portfolio>
}
