package ru.urent.qatools.data_opstask.impl

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.StandHttpClientProvider
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_opstask.OpsTaskRepository
import ru.urent.qatools.data_opstask.response.OpsTaskConfigResponse
import ru.urent.qatools.data_opstask.response.OpsTasksResponse
import ru.urent.qatools.data_opstask.response.TechZoneSettingsResponse

internal class OpsTaskRepositoryImpl(private val clientProvider: StandHttpClientProvider) : OpsTaskRepository {

    override suspend fun getConfig(): RequestResult<OpsTaskConfigResponse> = requestResultOf {
        clientProvider.httpClient.get("opstask/api/config/all").body()
    }

    override suspend fun getTechZoneSettings(): RequestResult<List<TechZoneSettingsResponse.Entry>> = requestResultOf {
        val entries = mutableListOf<TechZoneSettingsResponse.Entry>()
        var page = FIRST_PAGE
        do {
            val response: TechZoneSettingsResponse = clientProvider.httpClient.get("gateway-ocelot/api/v1/opstask-api/TechZoneSettings") {
                parameter(PAGE_PARAMETER, page)
                parameter(PAGE_SIZE_PARAMETER, SETTINGS_PAGE_SIZE)
            }.body()
            entries.addAll(response.entries.orEmpty())
            val totalPages = response.totalPages
            page += 1
        } while (totalPages != null && page <= totalPages)
        entries
    }

    override suspend fun getActiveChangeBatteryTasks(identifier: String): RequestResult<OpsTasksResponse> = requestResultOf {
        clientProvider.httpClient.get("gateway-ocelot/api/v1/opstask-api/Opstasks") {
            parameter(VEHICLE_IDS_PARAMETER, identifier)
            parameter(TYPE_PARAMETER, CHANGE_BATTERY_TYPE)
            parameter(SYSTEM_STATUS_PARAMETER, ACTIVE_SYSTEM_STATUS)
        }.body()
    }

    private companion object {
        const val PAGE_PARAMETER = "cPage"
        const val PAGE_SIZE_PARAMETER = "iOnPage"
        const val FIRST_PAGE = 1
        const val SETTINGS_PAGE_SIZE = 1000
        const val VEHICLE_IDS_PARAMETER = "VehicleIds"
        const val TYPE_PARAMETER = "Type"
        const val CHANGE_BATTERY_TYPE = "ChangeBattery"
        const val SYSTEM_STATUS_PARAMETER = "SystemStatus"
        const val ACTIVE_SYSTEM_STATUS = "Active"
    }
}
