package ru.urent.qatools.data_releases

import java.io.File
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_releases.response.ReleaseResponse

interface ReleasesRepository {
    suspend fun getLatestRelease(): RequestResult<ReleaseResponse>
    suspend fun downloadAsset(url: String, target: File, onProgress: (downloadedBytes: Long) -> Unit): RequestResult<Unit>
}
