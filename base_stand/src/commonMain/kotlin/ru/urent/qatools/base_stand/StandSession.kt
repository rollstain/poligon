package ru.urent.qatools.base_stand

import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.core_network.ReconnectingHttpClient

data class Stand(val baseUrl: String, val email: String?, val hasSavedLogin: Boolean)

data class StandConfig(
    val clientId: String,
    val clientSecret: String,
    val scope: String,
    val tileSource: TileSource?,
)

class StandSession(
    val baseUrl: String,
    val email: String,
    val config: StandConfig,
    val connection: ReconnectingHttpClient,
) {
    val host: String = baseUrl.substringAfter("://")
}

sealed interface LoginResult {
    data object Success : LoginResult

    data object SavedLoginExpired : LoginResult

    data object InvalidCredentials : LoginResult

    data object VpnRequired : LoginResult

    data object Unavailable : LoginResult

    data object SessionExpired : LoginResult

    data object SavedPasswordMissing : LoginResult

    data class Misconfigured(val message: String) : LoginResult

    data class Unexpected(val statusCode: Int?) : LoginResult
}
