package ru.urent.qatools.data_bikes.impl

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.StandHttpClientProvider
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_bikes.BikesRepository
import ru.urent.qatools.data_bikes.request.DisableReasonRequest
import ru.urent.qatools.data_bikes.response.BikeResponse
import ru.urent.qatools.data_bikes.response.DisabledReasonsResponse
import ru.urent.qatools.data_bikes.response.DriverTaskResponse
import ru.urent.qatools.data_bikes.response.TrackerResponse
import ru.urent.qatools.data_bikes.response.TrackerSettingsResponse
import ru.urent.qatools.data_bikes.response.TransportModelResponse

internal class BikesRepositoryImpl(private val clientProvider: StandHttpClientProvider) : BikesRepository {

    override suspend fun getTracker(imei: String): RequestResult<TrackerResponse> = requestResultOf {
        clientProvider.httpClient.get("trackers/api/trackers/tracker/$imei").body()
    }

    override suspend fun getBike(identifier: String): RequestResult<BikeResponse> = requestResultOf {
        clientProvider.httpClient.get("bikes/api/bikes/identifier/$identifier").body()
    }

    override suspend fun getTrackerSettings(imei: String): RequestResult<TrackerSettingsResponse> = requestResultOf {
        clientProvider.httpClient.get("drivernavtelecom/api/trackers/tracker/$imei").body()
    }

    override suspend fun getTransportModel(modelId: String): RequestResult<TransportModelResponse> = requestResultOf {
        clientProvider.httpClient.get("gateway-adm/api/bikes/models/$modelId") { parameter(WITH_EBIKES_PARAMETER, true) }.body()
    }

    override suspend fun getDriverTask(identifier: String): RequestResult<DriverTaskResponse> = requestResultOf {
        clientProvider.httpClient.get("gateway-ocelot/api/v1/pickupdropoff-api/tasks/byTransportId/$identifier").body()
    }

    override suspend fun getDisabledReasons(): RequestResult<DisabledReasonsResponse> = requestResultOf {
        clientProvider.httpClient.get("bikes/api/disabledreasons/allowed") { parameter(WITH_SYSTEM_PARAMETER, true) }.body()
    }

    override suspend fun replaceDisabledReason(
        bikeId: String,
        vendorId: String,
        reasonId: String,
        description: String,
    ): RequestResult<Unit> = requestResultOf {
        clientProvider.httpClient.post("bikes/api/status/disable-reason/set") {
            header(SELECTED_VENDORS_HEADER, vendorId)
            contentType(ContentType.Application.Json)
            setBody(DisableReasonRequest(ids = listOf(bikeId), reasonId = reasonId, description = description))
        }
        Unit
    }

    override suspend fun enable(bikeId: String, vendorId: String): RequestResult<Unit> = requestResultOf {
        clientProvider.httpClient.post("bikes/api/status/enable") {
            header(SELECTED_VENDORS_HEADER, vendorId)
            contentType(ContentType.Application.Json)
            setBody(listOf(bikeId))
        }
        Unit
    }

    private companion object {
        const val WITH_EBIKES_PARAMETER = "withEBikes"
        const val WITH_SYSTEM_PARAMETER = "withSystem"
        const val SELECTED_VENDORS_HEADER = "X-Selected-Vendors"
    }
}
