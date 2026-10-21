package ru.urent.qatools.core_network

import io.ktor.client.HttpClient

interface StandHttpClientProvider {
    val httpClient: HttpClient
}
