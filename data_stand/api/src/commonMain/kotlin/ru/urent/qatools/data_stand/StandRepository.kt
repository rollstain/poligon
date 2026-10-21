package ru.urent.qatools.data_stand

import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_stand.response.StandConfigResponse
import ru.urent.qatools.data_stand.response.TokenResponse

interface StandRepository {
    suspend fun getConfig(baseUrl: String): RequestResult<StandConfigResponse>
    suspend fun requestToken(
        baseUrl: String,
        clientId: String,
        clientSecret: String,
        scope: String,
        email: String,
        password: String,
    ): RequestResult<TokenResponse>
    suspend fun checkToken(baseUrl: String, accessToken: String): RequestResult<Unit>
    suspend fun getSavedStands(): List<SavedStandEntity>
    suspend fun getSavedStand(baseUrl: String): SavedStandEntity?
    suspend fun saveLogin(baseUrl: String, email: String, encryptedAccessToken: ByteArray, encryptedPassword: ByteArray?)
    suspend fun saveToken(baseUrl: String, encryptedAccessToken: ByteArray)
    suspend fun clearToken(baseUrl: String)
    suspend fun clearCredentials(baseUrl: String)
    suspend fun clearAllCredentials()
    suspend fun deleteStand(baseUrl: String)
}
