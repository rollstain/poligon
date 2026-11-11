package ru.urent.qatools.data_simulator.impl

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.ReconnectingHttpClient
import ru.urent.qatools.core_network.UnexpectedResponseException
import ru.urent.qatools.core_network.requestBodyOf
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_simulator.OmniEmulatorRepository

internal class OmniEmulatorRepositoryImpl(private val connection: ReconnectingHttpClient) : OmniEmulatorRepository {
    private val httpClient: HttpClient
        get() = connection.client

    override suspend fun isTrackerEmulated(baseUrl: String, imei: String): RequestResult<Boolean> = requestResultOf {
        val response = httpClient.get("$baseUrl$TRACKERS_PATH/$imei")
        when {
            response.status.isSuccess() -> true
            response.status == HttpStatusCode.NotFound -> false
            else -> throw UnexpectedResponseException(response.status.value, response.call.request.url.encodedPath)
        }
    }

    override suspend fun setChargeAndLocation(baseUrl: String, imei: String, chargePercent: Int, location: GeoPoint): RequestResult<Unit> =
        requestBodyOf {
            httpClient.post("$baseUrl$TRACKERS_PATH/$imei") {
                contentType(ContentType.Application.Json)
                setBody(
                    OmniTrackerConfigRequest(
                        chargePercentage = chargePercent,
                        forcedCoordinate = "${location.latitude}, ${location.longitude}",
                    ),
                )
            }
        }

    @Serializable
    private data class OmniTrackerConfigRequest(
        @SerialName("chargePercentage")
        val chargePercentage: Int,
        @SerialName("forcedCoordinate")
        val forcedCoordinate: String,
    )

    private companion object {
        const val TRACKERS_PATH = "/emulatoromni/api/trackers"
    }
}
