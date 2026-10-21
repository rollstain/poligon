package ru.urent.qatools.data_stand.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TokenErrorResponse(
    @SerialName("error")
    val error: String?,
)
