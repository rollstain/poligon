package ru.urent.qatools.data_releases.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseResponse(
    @SerialName("tag_name")
    val tagName: String?,
    val assets: List<ReleaseAssetResponse>?,
)
