package ru.urent.qatools.core_network.impl

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.testing_stand.RecordingLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class CachingHttpClientTest {

    @Test
    fun responseAllowedForCachingIsServedAgainFromStorage() = runTest {
        var requests = 0
        val engineFactory = TestEngineFactory {
            MockEngine {
                requests += 1
                respond(TILE_BODY, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, TILE_CACHE_CONTROL))
            }
        }
        val client = HttpClientFactoryImpl(engineFactory = UnusedEngineFactory, cachingEngineFactory = engineFactory, log = RecordingLog()).createCaching(storage = CacheStorage.Unlimited())

        val first = client.get(TILE_URL).bodyAsText()
        val second = client.get(TILE_URL).bodyAsText()

        assertEquals(TILE_BODY, first)
        assertEquals(TILE_BODY, second)
        assertEquals(1, requests)
    }

    private companion object {
        val UnusedEngineFactory = TestEngineFactory { error("Кеширующий клиент должен идти через свой движок") }
        const val TILE_URL = "https://tile1.test/17/79071/47870.png"
        const val TILE_BODY = "tile"
        const val TILE_CACHE_CONTROL = "max-age=86400, stale-while-revalidate=86400"
    }
}
