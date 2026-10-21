package ru.urent.qatools.core_network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.storage.CacheStorage

interface HttpClientFactory {
    fun createPlain(): HttpClient
    fun createCaching(storage: CacheStorage): HttpClient
    fun createStandFacing(onRejected: (standOrigin: String) -> Unit): ReconnectingHttpClient
    fun createForStand(
        baseUrl: String,
        accessToken: String,
        reauthenticate: suspend () -> String?,
        onSessionExpired: () -> Unit,
        onRejected: (standOrigin: String) -> Unit,
    ): ReconnectingHttpClient
}
