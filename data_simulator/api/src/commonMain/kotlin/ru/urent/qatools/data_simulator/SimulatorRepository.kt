package ru.urent.qatools.data_simulator

import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_simulator.response.SimulatorStatusResponse

interface SimulatorRepository {
    suspend fun getStatus(baseUrl: String, simulatorId: Int): RequestResult<SimulatorStatusResponse>
    suspend fun setLocation(baseUrl: String, simulatorId: Int, location: GeoPoint): RequestResult<Unit>
    suspend fun setVoltage(baseUrl: String, simulatorId: Int, millivolts: Int): RequestResult<Unit>
}
