package ru.urent.qatools.data_stand.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_database.CoreDatabase
import ru.urent.qatools.core_entities.RequestFailure
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.core_network.ReconnectingHttpClient
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_stand.SavedStandEntity
import ru.urent.qatools.data_stand.StandRepository
import ru.urent.qatools.data_stand.response.StandConfigResponse
import ru.urent.qatools.data_stand.response.TokenResponse

internal class StandRepositoryImpl(
    private val connection: ReconnectingHttpClient,
    database: CoreDatabase,
) : StandRepository {
    private val queries = database.standQueries
    private val httpClient: HttpClient
        get() = connection.client

    override suspend fun getConfig(baseUrl: String): RequestResult<StandConfigResponse> {
        val sent = requestResultOf { httpClient.get("$baseUrl$STAND_CONFIG_PATH") }
        val response = when (sent) {
            is RequestResult.Error -> return sent
            is RequestResult.Success -> sent.data
        }
        if (!response.status.isSuccess()) return RequestResult.Error(RequestFailure.Http(response.status.value))
        return requestResultOf { QaJson.decodeFromString<StandConfigResponse>(response.bodyAsText()) }
    }

    override suspend fun requestToken(
        baseUrl: String,
        clientId: String,
        clientSecret: String,
        scope: String,
        email: String,
        password: String,
    ): RequestResult<TokenResponse> {
        val sent = requestResultOf {
            httpClient.submitForm(
                url = "$baseUrl$TOKEN_PATH",
                formParameters = parameters {
                    append("grant_type", PASSWORD_GRANT_TYPE)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("scope", scope)
                    append("username", email)
                    append("password", password)
                },
            )
        }
        val response = when (sent) {
            is RequestResult.Error -> return sent
            is RequestResult.Success -> sent.data
        }
        if (response.status == HttpStatusCode.BadRequest) return RequestResult.Error(badRequestFailure(response))
        if (!response.status.isSuccess()) return RequestResult.Error(RequestFailure.Http(response.status.value))
        return requestResultOf { response.body<TokenResponse>() }
    }

    override suspend fun checkToken(baseUrl: String, accessToken: String): RequestResult<Unit> {
        val sent = requestResultOf { httpClient.get("$baseUrl$USER_INFO_PATH") { bearerAuth(accessToken) } }
        val response = when (sent) {
            is RequestResult.Error -> return sent
            is RequestResult.Success -> sent.data
        }
        if (!response.status.isSuccess()) return RequestResult.Error(RequestFailure.Http(response.status.value))
        return RequestResult.Success(Unit)
    }

    override suspend fun getSavedStands(): List<SavedStandEntity> = withContext(Dispatchers.IO) {
        val stands = mutableListOf<SavedStandEntity>()
        for (row in queries.selectAll().executeAsList()) {
            stands.add(
                SavedStandEntity(
                    baseUrl = row.base_url,
                    email = row.email,
                    encryptedAccessToken = row.encrypted_access_token,
                    encryptedPassword = row.encrypted_password,
                ),
            )
        }
        stands
    }

    override suspend fun getSavedStand(baseUrl: String): SavedStandEntity? = withContext(Dispatchers.IO) {
        val row = queries.selectByBaseUrl(baseUrl).executeAsOneOrNull()
        if (row == null) {
            null
        } else {
            SavedStandEntity(
                baseUrl = row.base_url,
                email = row.email,
                encryptedAccessToken = row.encrypted_access_token,
                encryptedPassword = row.encrypted_password,
            )
        }
    }

    override suspend fun saveLogin(baseUrl: String, email: String, encryptedAccessToken: ByteArray, encryptedPassword: ByteArray?): Unit =
        withContext(Dispatchers.IO) {
            queries.saveLogin(
                base_url = baseUrl,
                email = email,
                encrypted_access_token = encryptedAccessToken,
                encrypted_password = encryptedPassword,
            )
        }

    override suspend fun saveToken(baseUrl: String, encryptedAccessToken: ByteArray): Unit = withContext(Dispatchers.IO) {
        queries.saveToken(encrypted_access_token = encryptedAccessToken, base_url = baseUrl)
    }

    override suspend fun clearToken(baseUrl: String): Unit = withContext(Dispatchers.IO) {
        queries.clearToken(baseUrl)
    }

    override suspend fun clearCredentials(baseUrl: String): Unit = withContext(Dispatchers.IO) {
        queries.clearCredentials(baseUrl)
    }

    override suspend fun clearAllCredentials(): Unit = withContext(Dispatchers.IO) {
        queries.clearAllCredentials()
    }

    override suspend fun deleteStand(baseUrl: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteStand(baseUrl)
    }

    private suspend fun badRequestFailure(response: HttpResponse): RequestFailure {
        val error = requestResultOf { response.body<TokenErrorResponse>() }
        val isInvalidGrant = (error as? RequestResult.Success)?.data?.error == INVALID_GRANT_ERROR
        return if (isInvalidGrant) RequestFailure.InvalidCredentials else RequestFailure.Http(response.status.value)
    }

    private companion object {
        const val STAND_CONFIG_PATH = "/admin/assets/config.json"
        const val TOKEN_PATH = "/identity/connect/token"
        const val USER_INFO_PATH = "/identity/connect/userinfo"
        const val PASSWORD_GRANT_TYPE = "password"
        const val INVALID_GRANT_ERROR = "invalid_grant"
    }
}
