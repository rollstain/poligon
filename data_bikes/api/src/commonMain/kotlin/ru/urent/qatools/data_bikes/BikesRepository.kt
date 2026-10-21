package ru.urent.qatools.data_bikes

import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_bikes.response.BikeResponse
import ru.urent.qatools.data_bikes.response.DisabledReasonsResponse
import ru.urent.qatools.data_bikes.response.DriverTaskResponse
import ru.urent.qatools.data_bikes.response.TrackerResponse
import ru.urent.qatools.data_bikes.response.TrackerSettingsResponse
import ru.urent.qatools.data_bikes.response.TransportModelResponse

interface BikesRepository {
    suspend fun getTracker(imei: String): RequestResult<TrackerResponse>
    suspend fun getBike(identifier: String): RequestResult<BikeResponse>
    suspend fun getTrackerSettings(imei: String): RequestResult<TrackerSettingsResponse>
    suspend fun getTransportModel(modelId: String): RequestResult<TransportModelResponse>
    suspend fun getDriverTask(identifier: String): RequestResult<DriverTaskResponse>
    suspend fun getDisabledReasons(): RequestResult<DisabledReasonsResponse>
    suspend fun replaceDisabledReason(bikeId: String, vendorId: String, reasonId: String, description: String): RequestResult<Unit>
    suspend fun enable(bikeId: String, vendorId: String): RequestResult<Unit>
}
