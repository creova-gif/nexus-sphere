package com.nexussphere.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(val chart: YahooChart)

@Serializable
data class YahooChart(val result: List<YahooResult>? = null, val error: YahooError? = null)

@Serializable
data class YahooError(val code: String, val description: String)

@Serializable
data class YahooResult(
    val meta: YahooMeta,
    val timestamp: List<Long>? = null,
    val indicators: YahooIndicators? = null,
)

@Serializable
data class YahooMeta(
    val symbol: String,
    val regularMarketPrice: Double,
    val chartPreviousClose: Double? = null,
    val currency: String? = null,
    val exchangeName: String? = null,
)

@Serializable
data class YahooIndicators(val quote: List<YahooQuote>? = null)

@Serializable
data class YahooQuote(
    val close: List<Double?>? = null,
    val open: List<Double?>? = null,
    val high: List<Double?>? = null,
    val low: List<Double?>? = null,
    val volume: List<Long?>? = null,
)

@Serializable
data class YahooQuoteResponse(
    @SerialName("quoteResponse") val quoteResponse: YahooQuoteWrapper,
)

@Serializable
data class YahooQuoteWrapper(val result: List<YahooQuoteResult>? = null)

@Serializable
data class YahooQuoteResult(
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val trailingPE: Double? = null,
    val priceToBook: Double? = null,
    val revenueGrowth: Double? = null,
)
