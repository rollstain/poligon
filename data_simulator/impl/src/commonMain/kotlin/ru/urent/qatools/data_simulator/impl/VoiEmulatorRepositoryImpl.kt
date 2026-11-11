package ru.urent.qatools.data_simulator.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.ReconnectingHttpClient
import ru.urent.qatools.core_network.UnexpectedResponseException
import ru.urent.qatools.core_network.requestBodyOf
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_simulator.VoiEmulatorRepository
import ru.urent.qatools.data_simulator.response.VoiDeviceResponse

internal class VoiEmulatorRepositoryImpl(private val connection: ReconnectingHttpClient) : VoiEmulatorRepository {
    private val httpClient: HttpClient
        get() = connection.client

    override suspend fun getDevice(baseUrl: String, imei: String): RequestResult<VoiDeviceResponse?> = requestResultOf {
        val response = httpClient.get("$baseUrl$EMULATOR_PATH/$imei")
        if (response.status == HttpStatusCode.BadRequest) {
            null
        } else {
            successOrThrow(response).body<VoiDeviceResponse>()
        }
    }

    override suspend fun setBatteryPercent(baseUrl: String, imei: String, batteryPercent: Int): RequestResult<Unit> = requestResultOf {
        val deviceUrl = "$baseUrl$EMULATOR_PATH/$imei"
        val device = successOrThrow(httpClient.get(deviceUrl)).body<JsonObject>()
        val chargedDevice = JsonObject(device + (BATTERY_PERCENT_FIELD to JsonPrimitive(batteryPercent)))
        successOrThrow(
            httpClient.post(deviceUrl) {
                contentType(ContentType.Application.Json)
                setBody(chargedDevice)
            },
        )
        Unit
    }

    override suspend fun setLocation(baseUrl: String, imei: String, location: GeoPoint): RequestResult<Unit> = requestBodyOf {
        httpClient.post("$baseUrl$EMULATOR_PATH/$COORDINATES_PATH/$imei") {
            contentType(ContentType.Application.Json)
            setBody(VoiCoordinatesRequest(latitude = location.latitude, longitude = location.longitude))
        }
    }

    private fun successOrThrow(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw UnexpectedResponseException(response.status.value, response.call.request.url.encodedPath)
        return response
    }

    @Serializable
    private data class VoiCoordinatesRequest(
        @SerialName("lat")
        val latitude: Double,
        @SerialName("lng")
        val longitude: Double,
    )

    private companion object {
        const val EMULATOR_PATH = "/emulatorvoi/api/emulator"
        const val COORDINATES_PATH = "coordinates"
        const val BATTERY_PERCENT_FIELD = "batteryPercent"
    }
}
