package ru.urent.qatools.data_bikes.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.DriverTaskReason
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.data_bikes.response.BikeResponse
import ru.urent.qatools.data_bikes.response.DriverTaskResponse
import ru.urent.qatools.data_bikes.response.TrackerResponse
import ru.urent.qatools.data_bikes.response.TrackerSettingsResponse
import ru.urent.qatools.data_bikes.response.TransportModelResponse

class ResponseParsingTest {

    @Test
    fun transportModelFromStandHasNormalThresholds() {
        val model = QaJson.decodeFromString<TransportModelResponse>(fixture("/model.json"))
        val threshold = model.chargeThresholds.orEmpty().single()
        assertEquals("Ninebot L 60L", model.name)
        assertEquals(BatteryCapacityCategory.NORMAL, threshold?.batteryCapacityCategory)
        assertEquals(0.0, threshold?.batteryLevelToEmpty)
        assertEquals(10.0, threshold?.batteryLevelToPoweroff)
        assertEquals(15.0, threshold?.batteryLevelToDisable)
        assertEquals(25.0, threshold?.batteryLevelNeedToChange)
    }

    @Test
    fun navtelecomSettingsFromStandHaveOneBatteryRange() {
        val settings = QaJson.decodeFromString<TrackerSettingsResponse>(fixture("/tracker-driver.json"))
        assertEquals(33.4, settings.minVoltageOneBattery)
        assertEquals(41.5, settings.maxVoltageOneBattery)
    }

    @Test
    fun trackerFromStandHasBikeDriverLocationAndVoltage() {
        val tracker = QaJson.decodeFromString<TrackerResponse>(fixture("/tracker.json"))
        assertEquals("S.4071", tracker.identifier)
        assertEquals("driver.scooter.tracker.navtelecom.api", tracker.driver)
        assertEquals(43.424838333333334, tracker.location?.lat)
        assertEquals(32.0, tracker.telemetry?.values?.mainBatteryVoltage)
    }

    @Test
    fun knownChargeStatusIsParsed() {
        val bike = QaJson.decodeFromString<BikeResponse>(bikeJson(status = "ChargeIsLow"))
        assertEquals(ChargeState.LOW, bike.charge?.status)
        assertEquals(0.12506150610652986, bike.charge?.batteryFraction)
    }

    @Test
    fun unknownChargeStatusBecomesNull() {
        val bike = QaJson.decodeFromString<BikeResponse>(bikeJson(status = "SomethingNew"))
        assertNull(bike.charge?.status)
    }

    @Test
    fun disabledBikeFromStandHasIdReasonAndConnection() {
        val bike = QaJson.decodeFromString<BikeResponse>(fixture("/bike-disabled.json"))
        assertEquals("60c9a16d6450fb731f19324d", bike.id)
        assertEquals(true, bike.state?.flags?.isConnected)
        assertEquals(true, bike.state?.flags?.isDisabled)
        assertEquals(false, bike.state?.flags?.hasNoCoordinates)
        assertEquals("Готов к эксплуатации", bike.state?.mainDisabledReason)
        assertNull(bike.internalBattery)
    }

    @Test
    fun internalBatteryCategoryIsParsed() {
        val bike = QaJson.decodeFromString<BikeResponse>(
            """{"bikeBatteryInternal": {"bikeBatteryModel": {"batteryCapacityCategory": "Increased", "batteryCapacityCategoryName": "Усиленная"}}}""",
        )
        assertEquals(BatteryCapacityCategory.INCREASED, bike.internalBattery?.model?.capacityCategory)
    }

    @Test
    fun driverTaskFromStandHasReason() {
        val task = QaJson.decodeFromString<DriverTaskResponse>(fixture("/driver-task.json"))
        assertEquals("6ad244ed5602118e6c93866b", task.id)
        assertEquals(DriverTaskReason.READY_FOR_USE, task.reason)
    }

    @Test
    fun unknownDriverTaskReasonBecomesNull() {
        val task = QaJson.decodeFromString<DriverTaskResponse>("""{"id": "task", "reason": "SomethingNew"}""")
        assertNull(task.reason)
    }

    private fun bikeJson(status: String): String = """
        {
          "identifier": "S.4071",
          "model": {"id": "6065d2f5ef64db1143f1dec4", "name": "Ninebot L 60L"},
          "bikeCity": {"cityId": "5f9b2468f0e5ce0001b638e9", "cityName": "Адлер", "geoCode": "RU-KDA"},
          "charge": {"batteryPercent": 0.12506150610652986, "customerPercent": 0.0278, "status": "$status"}
        }
    """.trimIndent()

    private fun fixture(path: String): String =
        checkNotNull(javaClass.getResource(path)) { "Нет фикстуры $path" }.readText()
}
