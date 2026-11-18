package ru.urent.qatools.data_opstask

import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_opstask.response.OpsTaskConfigResponse
import ru.urent.qatools.data_opstask.response.OpsTasksResponse
import ru.urent.qatools.data_opstask.response.TechZoneSettingsResponse

interface OpsTaskRepository {
    suspend fun getConfig(): RequestResult<OpsTaskConfigResponse>
    suspend fun getTechZoneSettings(): RequestResult<List<TechZoneSettingsResponse.Entry>>
    suspend fun getActiveChangeBatteryTasks(identifier: String): RequestResult<OpsTasksResponse>
}
