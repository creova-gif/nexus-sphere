package com.nexussphere.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SnapTrade API response shapes — mirror the Flask proxy's JSON output. */

@Serializable
data class HoldingDto(
    val symbol: String,
    val units: Double,
    @SerialName("average_purchase_price") val averagePurchasePrice: Double? = null,
    val price: Double? = null,
    @SerialName("market_value") val marketValue: Double? = null,
    val currency: String = "CAD",
    @SerialName("open_pnl") val openPnl: Double? = null,
)

@Serializable
data class HoldingsResponseDto(
    val holdings: List<HoldingDto> = emptyList(),
)

@Serializable
data class PortfolioValueDto(
    val total: Double = 0.0,
    val currency: String = "CAD",
    val cash: Double = 0.0,
)

@Serializable
data class KillSwitchDto(
    @SerialName("killSwitch") val killSwitch: Boolean,
    @SerialName("maxOrderCAD") val maxOrderCad: Double,
)

@Serializable
data class RegisterResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("userSecret") val userSecret: String,
)

@Serializable
data class BrokerageDto(
    val id: String,
    val name: String,
    val slug: String = "",
    val url: String = "",
)

@Serializable
data class BrokeragesResponseDto(
    val brokerages: List<BrokerageDto> = emptyList(),
)
