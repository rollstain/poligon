package ru.urent.qatools.data_releases.impl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.UnexpectedResponseException
import ru.urent.qatools.core_network.requestBodyOf
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_releases.ReleasesRepository
import ru.urent.qatools.data_releases.response.ReleaseResponse

internal class ReleasesRepositoryImpl(private val httpClient: HttpClient) : ReleasesRepository {

    override suspend fun getLatestRelease(): RequestResult<ReleaseResponse> = requestBodyOf { httpClient.get(LATEST_RELEASE_URL) }

    override suspend fun downloadAsset(url: String, target: File, onProgress: (downloadedBytes: Long) -> Unit): RequestResult<Unit> =
        requestResultOf {
            httpClient.prepareGet(url) { timeout { requestTimeoutMillis = ASSET_DOWNLOAD_TIMEOUT_MS } }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw UnexpectedResponseException(response.status.value, response.call.request.url.encodedPath)
                }
                writeBody(channel = response.bodyAsChannel(), target = target, onProgress = onProgress)
            }
        }

    private suspend fun writeBody(channel: ByteReadChannel, target: File, onProgress: (downloadedBytes: Long) -> Unit) {
        withContext(Dispatchers.IO) {
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                var downloadedBytes = 0L
                var readBytes = channel.readAvailable(buffer)
                while (readBytes != END_OF_BODY) {
                    output.write(buffer, 0, readBytes)
                    downloadedBytes += readBytes
                    onProgress(downloadedBytes)
                    readBytes = channel.readAvailable(buffer)
                }
            }
        }
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/rollstain/poligon/releases/latest"
        const val ASSET_DOWNLOAD_TIMEOUT_MS = 30 * 60 * 1000L
        const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        const val END_OF_BODY = -1
    }
}
