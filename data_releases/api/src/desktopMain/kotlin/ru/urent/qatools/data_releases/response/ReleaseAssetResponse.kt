package ru.urent.qatools.data_releases.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseAssetResponse(
    val name: String?,
    val size: Long?,
    val digest: String?,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String?,
)
