package ru.urent.qatools.data_simulator.impl

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.core_network.ReconnectingHttpClient
import ru.urent.qatools.core_network.requestBodyOf
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_simulator.SimulatorRepository
import ru.urent.qatools.data_simulator.response.SimulatorStatusResponse

internal class SimulatorRepositoryImpl(private val connection: ReconnectingHttpClient) : SimulatorRepository {
    private val httpClient: HttpClient
        get() = connection.client

    override suspend fun getStatus(baseUrl: String, simulatorId: Int): RequestResult<SimulatorStatusResponse> {
        val text = requestBodyOf<String> {
            httpClient.get("$baseUrl$STATUS_PATH") { parameter(ID_PARAMETER, simulatorId) }
        }
        return when (text) {
            is RequestResult.Error -> text
            is RequestResult.Success -> requestResultOf { QaJson.decodeFromString<SimulatorStatusResponse>(text.data) }
        }
    }

    override suspend fun setLocation(baseUrl: String, simulatorId: Int, location: GeoPoint): RequestResult<Unit> =
        requestBodyOf {
            httpClient.post("$baseUrl$STATUS_PATH") {
                parameter(ID_PARAMETER, simulatorId)
                parameter(LATITUDE_PARAMETER, location.latitude.toString())
                parameter(LONGITUDE_PARAMETER, location.longitude.toString())
            }
        }

    override suspend fun setVoltage(baseUrl: String, simulatorId: Int, millivolts: Int): RequestResult<Unit> =
        requestBodyOf {
            httpClient.post("$baseUrl$STATUS_PATH") {
                parameter(ID_PARAMETER, simulatorId)
                parameter(VOLTAGE_PARAMETER, millivolts)
            }
        }

    private companion object {
        const val STATUS_PATH = "/simulator-signal/api/status"
        const val ID_PARAMETER = "id"
        const val LATITUDE_PARAMETER = "lat"
        const val LONGITUDE_PARAMETER = "lon"
        const val VOLTAGE_PARAMETER = "vol"
    }
}
