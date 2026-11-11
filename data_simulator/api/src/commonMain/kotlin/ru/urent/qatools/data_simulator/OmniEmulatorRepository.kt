package ru.urent.qatools.data_simulator

import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult

interface OmniEmulatorRepository {
    suspend fun isTrackerEmulated(baseUrl: String, imei: String): RequestResult<Boolean>
    suspend fun setChargeAndLocation(baseUrl: String, imei: String, chargePercent: Int, location: GeoPoint): RequestResult<Unit>
}
