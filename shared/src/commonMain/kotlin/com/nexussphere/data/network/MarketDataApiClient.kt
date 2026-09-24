package com.nexussphere.data.network

import com.nexussphere.data.dto.YahooChartResponse
import com.nexussphere.data.dto.YahooQuoteResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class MarketDataApiClient(private val http: HttpClient) {

    suspend fun fetchChart(symbol: String, range: String = "3mo", interval: String = "1d"): YahooChartResponse {
        val encodedSymbol = symbol.replace(".", "-")
        return http.get("https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol") {
            parameter("range", range)
            parameter("interval", interval)
            parameter("includePrePost", "false")
        }.body()
    }

    suspend fun fetchQuote(symbols: List<String>): YahooQuoteResponse {
        val joined = symbols.joinToString(",") { it.replace(".", "-") }
        return http.get("https://query1.finance.yahoo.com/v7/finance/quote") {
            parameter("symbols", joined)
            parameter("fields", "regularMarketPrice,regularMarketChangePercent,trailingPE,priceToBook,revenueGrowth")
        }.body()
    }
}
