package ru.urent.qatools.base_map

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.testing_stand.RecordingLog

class MapTileLoaderTest {
    private val log = RecordingLog()

    @Test
    fun tileIsRequestedBySourceTemplateAndReturnedAsIs() = runTest {
        val requestedUrls = mutableListOf<String>()
        val loader = loader(MockEngine { request ->
            requestedUrls.add(request.url.toString())
            respond(TILE_BYTES, HttpStatusCode.OK)
        })

        val tile = assertNotNull(loader.loadTile(Source, row = ROW, col = COL, zoomLevel = ZOOM))

        assertEquals(listOf("https://tile2.test/$ZOOM/$COL/$ROW.png?key=$API_KEY"), requestedUrls)
        val buffer = Buffer()
        tile.readAtMostTo(buffer, Long.MAX_VALUE)
        assertTrue(TILE_BYTES.contentEquals(buffer.readByteArray()))
        assertEquals(emptyList(), log.lines)
    }

    @Test
    fun rejectedTileIsLoggedWithStatusAndWithoutKey() = runTest {
        val loader = loader(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })

        val tile = loader.loadTile(Source, row = ROW, col = COL, zoomLevel = ZOOM)

        assertNull(tile)
        assertEquals(listOf("Тайл $ZOOM/$COL/$ROW не загружен: HTTP 503"), log.lines)
    }

    @Test
    fun failingRequestIsLoggedInsteadOfBreakingTileLoading() = runTest {
        val loader = loader(MockEngine { throw IllegalArgumentException(BROKEN_RESPONSE) })

        val tile = loader.loadTile(Source, row = ROW, col = COL, zoomLevel = ZOOM)

        assertNull(tile)
        assertEquals(listOf("Тайл $ZOOM/$COL/$ROW не загружен"), log.lines)
    }

    private fun loader(engine: MockEngine) = MapTileLoader(httpClient = HttpClient(engine), log = log)

    private companion object {
        const val ROW = 47870
        const val COL = 79071
        const val ZOOM = 17
        const val API_KEY = "secret"
        const val BROKEN_RESPONSE = "Неожиданный ответ"
        val TILE_BYTES = byteArrayOf(1, 2, 3)
        val Source = TileSource(urlTemplate = "https://tile{s}.test/{z}/{x}/{y}.png", subdomains = listOf("1", "2"), urlSuffix = "?key=$API_KEY")
    }
}
