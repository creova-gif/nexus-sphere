package com.nexussphere.data.network

import com.nexussphere.data.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * HTTP client for the NexusSphere Flask server.
 * All credential-bearing calls use POST body — never query parameters.
 */
class NexusApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
) {
    @Serializable
    private data class CredsBody(val userId: String, val userSecret: String)

    suspend fun getHoldings(userId: String, userSecret: String): HoldingsResponseDto =
        http.post("$baseUrl/api/snap/holdings") {
            contentType(ContentType.Application.Json)
            setBody(CredsBody(userId, userSecret))
        }.body()

    suspend fun getKillSwitch(): KillSwitchDto =
        http.get("$baseUrl/api/snap/kill-switch").body()

    suspend fun getBrokerages(): BrokeragesResponseDto =
        http.get("$baseUrl/api/snap/brokerages").body()

    companion object {
        /** Base URL for local dev. Override with NEXUS_SERVER_URL env var or build config. */
        const val LOCAL_BASE_URL    = "http://localhost:5000"
        const val REPLIT_BASE_URL   = "https://nexussphere.repl.co"
    }
}
