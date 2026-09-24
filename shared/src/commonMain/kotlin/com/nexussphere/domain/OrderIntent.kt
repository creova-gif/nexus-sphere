package com.nexussphere.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class OrderAction { BUY, SELL }

@Serializable
enum class OrderType { MARKET, LIMIT, STOP, STOP_LIMIT }

/**
 * Client-side expression of trading intent.
 * This is NEVER executed directly — it must pass server-side [RiskDecision] first.
 */
@Serializable
data class OrderIntent(
    val symbol: Symbol,
    val action: OrderAction,
    val units: Double,
    val limitPrice: Money?,
    val orderType: OrderType,
    val strategyId: String?,
    val signalScore: Int?,
    val createdAt: Instant,
) {
    fun notional(marketPrice: Money): Money = marketPrice * units
}

/** Result of the server-side order preview (impact check). */
@Serializable
data class OrderPreview(
    val tradeId: String,
    val intent: OrderIntent,
    val estimatedFill: Money,
    val estimatedCommission: Money,
    val estimatedTotal: Money,
    val warningMessages: List<String>,
    val expiresAt: Instant,
)
