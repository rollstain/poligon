package ru.urent.qatools.base_map

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.SkipRequestLog

class MapTileLoader(private val httpClient: HttpClient, private val log: AppLog) {

    suspend fun loadTile(source: TileSource, row: Int, col: Int, zoomLevel: Int): RawSource? {
        val tileName = "$zoomLevel/$col/$row"
        val subdomain = if (source.subdomains.isEmpty()) "" else source.subdomains[(row + col) % source.subdomains.size]
        val placeholders = mapOf(
            "{s}" to subdomain,
            "{z}" to zoomLevel.toString(),
            "{x}" to col.toString(),
            "{y}" to row.toString(),
        )
        var url = source.urlTemplate
        for ((placeholder, value) in placeholders) url = url.replace(placeholder, value)
        val bytes = try {
            val response = httpClient.get(url + source.urlSuffix) { attributes.put(SkipRequestLog, Unit) }
            if (!response.status.isSuccess()) {
                log.error(message = "Тайл $tileName не загружен: HTTP ${response.status.value}", cause = null)
                return null
            }
            response.bodyAsBytes()
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            log.error(message = "Тайл $tileName не загружен", cause = exception)
            return null
        }
        if (bytes.isEmpty()) {
            log.error(message = "Тайл $tileName пришёл пустым", cause = null)
            return null
        }
        val buffer = Buffer()
        buffer.write(bytes)
        return buffer
    }
}
