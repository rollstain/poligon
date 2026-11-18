package ru.urent.qatools.base_bike_setup.domain.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.PriorityConfig
import ru.urent.qatools.core_entities.TaskPriority
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.data_opstask.response.OpsTaskConfigResponse
import ru.urent.qatools.data_opstask.response.OpsTasksResponse

class OpsTaskMapperTest {
    private val mapper = OpsTaskMapper()

    @Test
    fun configOfStandTakesChangedValuesAndDefaultsForUnchanged() {
        val response = QaJson.decodeFromString<OpsTaskConfigResponse>(fixture("/opstask-config.json"))

        val config = mapper.toPriorityConfig(response)

        val expected = PriorityConfig(
            criticalPercent = 4,
            burningFallbackPercent = 10,
            highFallbackPercent = 30,
            defaultZonePercent = 45,
            isIncreasedBatteryEnabled = false,
            increasedBatteryDivider = 1.1,
        )
        assertEquals(expected, config)
    }

    @Test
    fun configWithoutKeysUsesBackendCodeDefaults() {
        val config = mapper.toPriorityConfig(OpsTaskConfigResponse(entries = emptyList()))

        val expected = PriorityConfig(
            criticalPercent = 0,
            burningFallbackPercent = 5,
            highFallbackPercent = 27,
            defaultZonePercent = 45,
            isIncreasedBatteryEnabled = false,
            increasedBatteryDivider = 1.0,
        )
        assertEquals(expected, config)
    }

    @Test
    fun enabledIncreasedBatteryAndZeroDividerAreReadLikeBackend() {
        val response = OpsTaskConfigResponse(
            entries = listOf(
                OpsTaskConfigResponse.Entry(name = "IncreasedBattery:IsEnabled", data = "true", defaultData = "False"),
                OpsTaskConfigResponse.Entry(name = "IncreasedBattery:DividerForNormalPriority", data = "0", defaultData = "1.4"),
            ),
        )

        val config = mapper.toPriorityConfig(response)

        assertEquals(true, config.isIncreasedBatteryEnabled)
        assertEquals(1.0, config.increasedBatteryDivider)
    }

    @Test
    fun taskOfBikeIsAbsentActiveOrUnknown() {
        assertEquals(BatteryTask.Absent, mapper.toBatteryTask(OpsTasksResponse(entries = emptyList())))
        assertEquals(
            BatteryTask.Active(TaskPriority.HIGH),
            mapper.toBatteryTask(QaJson.decodeFromString<OpsTasksResponse>("""{"entries":[{"priority":"High","rank":0}]}""")),
        )
        assertEquals(BatteryTask.Unknown, mapper.toBatteryTask(QaJson.decodeFromString<OpsTasksResponse>("""{"entries":[{"priority":"HighOld"}]}""")))
    }

    private fun fixture(path: String): String = checkNotNull(javaClass.getResource(path)) { "Нет фикстуры $path" }.readText()
}
