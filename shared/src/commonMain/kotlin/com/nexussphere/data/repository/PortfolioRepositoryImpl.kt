package com.nexussphere.data.repository

import com.nexussphere.data.network.NexusApiClient
import com.nexussphere.domain.*
import kotlinx.datetime.Clock

/**
 * Live implementation: calls Flask → SnapTrade → Wealthsimple.
 * Falls back to [DemoPortfolioRepository] on any network failure.
 */
class PortfolioRepositoryImpl(
    private val api: NexusApiClient,
    private val demo: PortfolioRepository = DemoPortfolioRepository(),
) : PortfolioRepository {

    override suspend fun getPositions(userId: String, userSecret: String): Result<List<Position>> =
        runCatching {
            val dto = api.getHoldings(userId, userSecret)
            dto.holdings.mapNotNull { h ->
                runCatching {
                    val sym      = Symbol(h.symbol.uppercase())
                    val price    = h.price ?: return@runCatching null
                    val avgCost  = h.averagePurchasePrice ?: price
                    val currency = if (h.currency == "USD") Currency.USD else Currency.CAD
                    val priceCad = if (currency == Currency.USD) price / CAD_USD_RATE else price
                    val avgCad   = if (currency == Currency.USD) avgCost / CAD_USD_RATE else avgCost
                    Position.of(sym, h.units, avgCad, priceCad, currency)
                }.getOrNull()
            }
        }.recoverCatching { demo.getPositions(userId, userSecret).getOrThrow() }

    override suspend fun getPortfolio(userId: String, userSecret: String): Result<Portfolio> =
        getPositions(userId, userSecret).mapCatching { positions ->
            Portfolio.fromPositions(
                accountId   = "LIVE-TFSA",
                accountName = "Wealthsimple TFSA",
                positions   = positions,
                asOf        = Clock.System.now(),
            )
        }.recoverCatching { demo.getPortfolio(userId, userSecret).getOrThrow() }
}
