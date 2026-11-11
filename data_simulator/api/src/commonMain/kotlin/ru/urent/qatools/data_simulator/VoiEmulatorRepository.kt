package ru.urent.qatools.data_simulator

import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_simulator.response.VoiDeviceResponse

interface VoiEmulatorRepository {
    suspend fun getDevice(baseUrl: String, imei: String): RequestResult<VoiDeviceResponse?>
    suspend fun setBatteryPercent(baseUrl: String, imei: String, batteryPercent: Int): RequestResult<Unit>
    suspend fun setLocation(baseUrl: String, imei: String, location: GeoPoint): RequestResult<Unit>
}
