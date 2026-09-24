package com.nexussphere.data.network

import io.ktor.client.*
import io.ktor.client.engine.android.*

actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    engine {
        connectTimeout = 10_000
        socketTimeout  = 30_000
    }
}
