package com.nexussphere.data.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/** Platform-specific engine supplied via expect/actual. */
expect fun createHttpClient(): HttpClient

val nexusJson = Json {
    ignoreUnknownKeys = true
    isLenient         = true
    coerceInputValues = true
}

/** Shared Ktor client config — engine is platform-specific. */
fun buildNexusHttpClient(): HttpClient = createHttpClient().config {
    install(ContentNegotiation) { json(nexusJson) }
    install(Logging) {
        level  = LogLevel.HEADERS
        logger = Logger.DEFAULT
    }
}
