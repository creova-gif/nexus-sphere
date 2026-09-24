package com.nexussphere.data.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    engine {
        requestTimeout = 30_000
    }
}
