package ru.urent.qatools.base_stand

import io.ktor.client.HttpClient
import ru.urent.qatools.core_network.StandHttpClientProvider

internal class StandSessionHttpClientProvider(private val sessionInteractor: StandSessionInteractor) : StandHttpClientProvider {
    override val httpClient: HttpClient
        get() = checkNotNull(sessionInteractor.session.value) { "Нет открытой сессии стенда" }.connection.client
}
