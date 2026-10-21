package ru.urent.qatools.core_network

import io.ktor.client.HttpClient
import kotlin.concurrent.Volatile

class ReconnectingHttpClient(private val createClient: (reconnect: () -> Unit) -> HttpClient) {
    @Volatile
    var client: HttpClient = createClient(::reconnect)
        private set

    fun close() = client.close()

    private fun reconnect() {
        synchronized(this) {
            val rejectedClient = client
            client = createClient(::reconnect)
            rejectedClient.close()
        }
    }
}
