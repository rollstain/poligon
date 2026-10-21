package ru.urent.qatools.data_stand.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StandConfigResponse(
    @SerialName("api")
    val api: Api?,
    @SerialName("auth")
    val auth: Auth?,
) {
    @Serializable
    data class Api(
        @SerialName("tileServers")
        val tileServers: Map<String, TileServer>?,
    )

    @Serializable
    data class TileServer(
        @SerialName("tileServerUrl")
        val tileServerUrl: String?,
        @SerialName("apiKey")
        val apiKey: String?,
        @SerialName("subdomains")
        val subdomains: String?,
    )

    @Serializable
    data class Auth(
        @SerialName("clientId")
        val clientId: String?,
        @SerialName("clientSecret")
        val clientSecret: String?,
        @SerialName("scope")
        val scope: String?,
    )
}
