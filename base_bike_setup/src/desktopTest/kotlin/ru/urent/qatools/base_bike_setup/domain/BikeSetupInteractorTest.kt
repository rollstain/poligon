package ru.urent.qatools.base_bike_setup.domain

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import ru.urent.qatools.base_bike_setup.entities.ApplyFailure
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.DriverTaskReason
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.testing_stand.TestStandInteractors

class BikeSetupInteractorTest {

    @Test
    fun lowTargetSendsLocationAndVoltageAndConfirmsLowState() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario())
        val location = GeoPoint(43.42485, 39.92816)

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Keep, location)).toList()

        assertEquals(ApplyStatus.Resolving, statuses.first())
        assertEquals(ApplyStatus.WaitingForTracker(12.5), statuses[1])
        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
        assertEquals(ChargeState.LOW, done.state)
        assertTrue(done.isTargetReached)
        assertEquals(AvailabilityTarget.Keep, done.availability)
        val locationPost = stand.simulatorPosts.first { it["lat"] != null }
        assertEquals(location.latitude.toString(), locationPost["lat"])
        assertEquals(location.longitude.toString(), locationPost["lon"])
        assertEquals(stand.lastMillivolts.toString(), stand.simulatorPosts.first { it["vol"] != null }["vol"])
        assertTrue(stand.availabilityRequests.isEmpty())
    }

    @Test
    fun resultReportsSentPercentWhileStandStillShowsPreviousCharge() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(stalePercent = STALE_PERCENT))

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Keep)).toList()

        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
        assertEquals(LOW_TARGET_PERCENT, done.percent)
    }

    @Test
    fun disabledTargetReplacesReasonsOfBikeVendorAfterChargeIsConfirmed() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(disabledReason = "Низкий заряд батареи"))
        val needsRepair = AvailabilityTarget.Disabled(reasonName = "NeedsRepair", reasonTitle = "Требует ремонта")

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.NORMAL), needsRepair)).toList()

        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
        assertEquals(needsRepair, done.availability)
        val request = stand.availabilityRequests.single()
        assertEquals("/bikes/api/status/disable-reason/set", request.path)
        assertEquals(VENDOR_ID, request.vendors)
        val body = Json.parseToJsonElement(request.body) as JsonObject
        assertEquals(listOf(BIKE_ID), body.getValue("ids").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(NEEDS_REPAIR_REASON_ID, body.getValue("reasonId").jsonPrimitive.content)
        assertEquals("Полигон", body.getValue("description").jsonPrimitive.content)
    }

    @Test
    fun availableTargetPutsBikeIntoAvailability() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(disabledReason = "Требует ремонта"))

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.NORMAL), AvailabilityTarget.Available)).toList()

        assertTrue(statuses.last() is ApplyStatus.Done, "last status: ${statuses.last()}")
        val request = stand.availabilityRequests.single()
        assertEquals("/bikes/api/status/enable", request.path)
        assertEquals(VENDOR_ID, request.vendors)
        assertEquals(listOf(BIKE_ID), (Json.parseToJsonElement(request.body) as JsonArray).map { it.jsonPrimitive.content })
    }

    @Test
    fun standIgnoringAvailabilityChangeIsReportedWithCurrentReason() = runTest {
        val scenario = StandScenario(disabledReason = "Низкий заряд батареи", isIgnoringAvailability = true)
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)
        val needsRepair = AvailabilityTarget.Disabled(reasonName = "NeedsRepair", reasonTitle = "Требует ремонта")

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.NORMAL), needsRepair)).toList()

        val expected = ApplyFailure.AvailabilityIgnored(isDisabled = true, currentReason = "Низкий заряд батареи")
        assertEquals(ApplyStatus.Failed(expected), statuses.last())
    }

    @Test
    fun reasonMissingOnStandIsReportedAndAvailabilityIsUntouched() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario())
        val missing = AvailabilityTarget.Disabled(reasonName = "NoSuchReason", reasonTitle = "Несуществующая причина")

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.NORMAL), missing)).toList()

        assertEquals(ApplyStatus.Failed(ApplyFailure.ReasonNotFound("Несуществующая причина")), statuses.last())
        assertTrue(stand.availabilityRequests.isEmpty())
    }

    @Test
    fun failedAvailabilityChangeSaysChargeIsAlreadySet() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(availabilityStatus = HttpStatusCode.InternalServerError))

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.NORMAL), AvailabilityTarget.Available)).toList()

        val expected = ApplyFailure.AvailabilityNotChanged(ApplyFailure.Unexpected(HttpStatusCode.InternalServerError.value))
        assertEquals(ApplyStatus.Failed(expected), statuses.last())
    }

    @Test
    fun lookupReadsLiveStatusAndDriverTask() = runTest {
        val scenario = StandScenario(
            isConnected = false,
            connectionLostAt = "2026-10-16T14:05:00Z",
            isGpsLost = true,
            gpsLostSince = "2026-10-16T13:58:00Z",
            disabledReason = "Готов к эксплуатации",
            driverTaskStatus = HttpStatusCode.OK,
        )
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertTrue(lookup is BikeLookup.Found, "lookup: $lookup")
        val status = lookup.bike.status
        assertEquals(BIKE_ID, lookup.bike.bikeId)
        assertEquals(false, status.isOnline)
        assertEquals(Instant.parse("2026-10-16T14:05:00Z"), status.offlineSince)
        assertTrue(status.isGpsLost)
        assertEquals(Instant.parse("2026-10-16T13:58:00Z"), status.gpsLostSince)
        assertEquals(true, status.isDisabled)
        assertEquals("Готов к эксплуатации", status.disabledReason)
        assertEquals(BatteryCapacityCategory.NOT_SET, status.batteryCategory)
        assertEquals(DriverTask(DriverTaskReason.READY_FOR_USE), status.driverTask)
    }

    @Test
    fun failingDriverTaskRequestDoesNotBreakLookup() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(driverTaskStatus = HttpStatusCode.InternalServerError))

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertTrue(lookup is BikeLookup.Found, "lookup: $lookup")
        assertNull(lookup.bike.status.driverTask)
    }

    @Test
    fun increasedBatteryUsesIncreasedThresholdsOfModel() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(batteryCategory = "Increased", model = TWO_CATEGORY_MODEL_JSON))

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertTrue(lookup is BikeLookup.Found, "lookup: $lookup")
        assertEquals(BatteryCapacityCategory.INCREASED, lookup.bike.status.batteryCategory)
        assertEquals(ChargeThresholds(emptyPercent = 0.0, powerOffPercent = 6.0, disablePercent = 11.0, needToChangePercent = 15.0), lookup.bike.thresholds)
    }

    @Test
    fun unknownTrackerIsReportedAsTrackerNotFound() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(trackerStatus = HttpStatusCode.BadRequest))

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertEquals(BikeLookup.Failed(ApplyFailure.TrackerNotFound), lookup)
        assertTrue(stand.simulatorPosts.isEmpty())
    }

    @Test
    fun trackerOfOtherDriverIsNotSupported() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(trackerDriver = LIME_DRIVER))

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.Percent(50.0), AvailabilityTarget.Keep)).toList()

        assertEquals(ApplyStatus.Failed(ApplyFailure.UnsupportedDriver(LIME_DRIVER)), statuses.last())
        assertTrue(stand.simulatorPosts.isEmpty())
    }

    @Test
    fun missingBikeIsReportedAsBikeNotFound() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(bikeStatus = HttpStatusCode.NotFound))

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertEquals(BikeLookup.Failed(ApplyFailure.BikeNotFound), lookup)
    }

    @Test
    fun navtelecomTrackerOutsideSimulatorIsNotEmulated() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(trackerImei = REAL_NAVTELECOM_IMEI))

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertEquals(BikeLookup.Failed(ApplyFailure.TrackerNotEmulated(REAL_NAVTELECOM_IMEI)), lookup)
    }

    @Test
    fun omniBikeGetsWholePercentAndCoordinateFromEmulator() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(trackerDriver = OMNI_DRIVER, trackerImei = OMNI_IMEI))
        val location = GeoPoint(43.42485, 39.92816)

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Keep, location)).toList()

        assertEquals(ApplyStatus.WaitingForTracker(LOW_WHOLE_PERCENT), statuses[1])
        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
        assertEquals(ChargeState.LOW, done.state)
        assertTrue(done.isTargetReached)
        val body = Json.parseToJsonElement(stand.omniPosts.single()) as JsonObject
        assertEquals(LOW_WHOLE_PERCENT.toInt().toString(), body.getValue("chargePercentage").jsonPrimitive.content)
        assertEquals("43.42485, 39.92816", body.getValue("forcedCoordinate").jsonPrimitive.content)
        assertTrue(stand.simulatorPosts.isEmpty())
    }

    @Test
    fun omniChargeArrivingOnSecondReportAfterChangeIsConfirmed() = runTest {
        val scenario = StandScenario(trackerDriver = OMNI_DRIVER, trackerImei = OMNI_IMEI, emulatorChargeReportDelay = OMNI_OBSERVED_CHARGE_DELAY)
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Keep)).toList()

        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
    }

    @Test
    fun omniTrackerUnknownToEmulatorIsNotEmulated() = runTest {
        val scenario = StandScenario(trackerDriver = OMNI_DRIVER, trackerImei = OMNI_IMEI, isTrackerEmulated = false)
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)

        val statuses = stand.interactor().apply(standBaseUrl = BASE_URL, setup = setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Keep)).toList()

        assertEquals(ApplyStatus.Failed(ApplyFailure.TrackerNotEmulated(OMNI_IMEI)), statuses.last())
        assertTrue(stand.omniPosts.isEmpty())
    }

    @Test
    fun voiBikeKeepsOtherDeviceSettingsWhenChargeChanges() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario(trackerDriver = VOI_DRIVER, trackerImei = VOI_IMEI))
        val location = GeoPoint(43.42485, 39.92816)

        val statuses = stand.interactor().apply(
            standBaseUrl = BASE_URL,
            setup = setup(ChargeTarget.State(ChargeState.NEED_TO_CHANGE), AvailabilityTarget.Keep, location),
        ).toList()

        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
        assertEquals(ChargeState.NEED_TO_CHANGE, done.state)
        val original = Json.parseToJsonElement(VOI_DEVICE_JSON) as JsonObject
        val sent = Json.parseToJsonElement(stand.voiDevicePosts.single()) as JsonObject
        assertEquals(original.keys, sent.keys)
        assertEquals(original - "batteryPercent", sent - "batteryPercent")
        assertEquals(NEED_TO_CHANGE_WHOLE_PERCENT.toString(), sent.getValue("batteryPercent").jsonPrimitive.content)
        val coordinates = Json.parseToJsonElement(stand.voiCoordinatePosts.single()) as JsonObject
        assertEquals(location.latitude.toString(), coordinates.getValue("lat").jsonPrimitive.content)
        assertEquals(location.longitude.toString(), coordinates.getValue("lng").jsonPrimitive.content)
    }

    @Test
    fun voiChargeArrivingOnSecondReportAfterChangeIsConfirmed() = runTest {
        val scenario = StandScenario(trackerDriver = VOI_DRIVER, trackerImei = VOI_IMEI, emulatorChargeReportDelay = VOI_OBSERVED_CHARGE_DELAY)
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)

        val statuses = stand.interactor().apply(
            standBaseUrl = BASE_URL,
            setup = setup(ChargeTarget.State(ChargeState.NEED_TO_CHANGE), AvailabilityTarget.Keep),
        ).toList()

        val done = statuses.last()
        assertTrue(done is ApplyStatus.Done, "last status: $done")
    }

    @Test
    fun voiTrackerNotRunningInEmulatorIsNotEmulated() = runTest {
        val scenario = StandScenario(trackerDriver = VOI_DRIVER, trackerImei = VOI_IMEI, isTrackerEmulated = false)
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)

        val lookup = stand.interactor().lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER)

        assertEquals(BikeLookup.Failed(ApplyFailure.TrackerNotEmulated(VOI_IMEI)), lookup)
    }

    @Test
    fun recalculatedChargeReappliesChosenReasonWhenStandLeftLowBattery() = runTest {
        val scenario = StandScenario(
            disabledReason = READY_FOR_USE_TITLE,
            disabledReasonTags = listOf(READY_FOR_USE_TAG, LOW_BATTERY_TAG),
            initialMillivolts = NORMAL_MILLIVOLTS,
        )
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)
        val interactor = stand.interactor()
        val bike = (interactor.lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER) as BikeLookup.Found).bike
        val readyForUse = AvailabilityTarget.Disabled(reasonName = READY_FOR_USE_TAG, reasonTitle = READY_FOR_USE_TITLE)
        val waiting = ApplyStatus.Done(percent = 62.5, state = ChargeState.LOW, isTargetReached = false, availability = readyForUse)

        val result = interactor.confirmRecalculatedCharge(setup(ChargeTarget.State(ChargeState.NORMAL), readyForUse), waiting, bike)

        assertTrue(bike.status.hasLowBatteryReason)
        assertEquals(waiting.copy(state = ChargeState.NORMAL, isTargetReached = true), result)
        val request = stand.availabilityRequests.single()
        assertEquals(DISABLE_REASON_PATH, request.path)
        assertEquals(READY_FOR_USE_REASON_ID, (Json.parseToJsonElement(request.body) as JsonObject).getValue("reasonId").jsonPrimitive.content)
    }

    @Test
    fun recalculatedChargeWithoutLowBatteryOnlyConfirmsResult() = runTest {
        val scenario = StandScenario(
            disabledReason = READY_FOR_USE_TITLE,
            disabledReasonTags = listOf(READY_FOR_USE_TAG),
            initialMillivolts = NORMAL_MILLIVOLTS,
        )
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)
        val interactor = stand.interactor()
        val bike = (interactor.lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER) as BikeLookup.Found).bike
        val readyForUse = AvailabilityTarget.Disabled(reasonName = READY_FOR_USE_TAG, reasonTitle = READY_FOR_USE_TITLE)
        val waiting = ApplyStatus.Done(percent = 62.5, state = ChargeState.LOW, isTargetReached = false, availability = readyForUse)

        val result = interactor.confirmRecalculatedCharge(setup(ChargeTarget.State(ChargeState.NORMAL), readyForUse), waiting, bike)

        assertEquals(waiting.copy(state = ChargeState.NORMAL, isTargetReached = true), result)
        assertTrue(stand.availabilityRequests.isEmpty())
    }

    @Test
    fun lowTargetKeepsLowBatteryReasonThatStandSetByItsRules() = runTest {
        val scenario = StandScenario(
            disabledReason = LOW_BATTERY_TITLE,
            disabledReasonTags = listOf(LOW_BATTERY_TAG),
            initialMillivolts = LOW_MILLIVOLTS,
        )
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)
        val interactor = stand.interactor()
        val bike = (interactor.lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER) as BikeLookup.Found).bike
        val waiting = ApplyStatus.Done(percent = 12.5, state = ChargeState.NORMAL, isTargetReached = false, availability = AvailabilityTarget.Available)

        val result = interactor.confirmRecalculatedCharge(setup(ChargeTarget.State(ChargeState.LOW), AvailabilityTarget.Available), waiting, bike)

        assertEquals(waiting.copy(state = ChargeState.LOW, isTargetReached = true), result)
        assertTrue(stand.availabilityRequests.isEmpty())
    }

    @Test
    fun chargeNotYetRecalculatedKeepsWaitingAndLeavesAvailability() = runTest {
        val scenario = StandScenario(disabledReason = READY_FOR_USE_TITLE, disabledReasonTags = listOf(READY_FOR_USE_TAG, LOW_BATTERY_TAG))
        val stand = FakeStand(StandardTestDispatcher(testScheduler), scenario)
        val interactor = stand.interactor()
        val bike = (interactor.lookup(standBaseUrl = BASE_URL, bikeNumber = BIKE_NUMBER) as BikeLookup.Found).bike
        val readyForUse = AvailabilityTarget.Disabled(reasonName = READY_FOR_USE_TAG, reasonTitle = READY_FOR_USE_TITLE)
        val waiting = ApplyStatus.Done(percent = 62.5, state = ChargeState.LOW, isTargetReached = false, availability = readyForUse)

        val result = interactor.confirmRecalculatedCharge(setup(ChargeTarget.State(ChargeState.NORMAL), readyForUse), waiting, bike)

        assertEquals(waiting, result)
        assertTrue(stand.availabilityRequests.isEmpty())
    }

    @Test
    fun disabledReasonsFromStandKeepVisibleReasonsWithTitles() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler), StandScenario())

        val reasons = stand.interactor().getDisabledReasons()

        assertEquals(DISABLED_REASONS_ON_STAND, reasons.size)
        assertTrue(DisabledReason(id = NEEDS_REPAIR_REASON_ID, name = "NeedsRepair", title = "Требует ремонта") in reasons)
        assertTrue(DisabledReason(id = READY_FOR_USE_REASON_ID, name = READY_FOR_USE_TAG, title = READY_FOR_USE_TITLE) in reasons)
    }

    private fun setup(target: ChargeTarget, availability: AvailabilityTarget, location: GeoPoint = GeoPoint(43.0, 39.0)) =
        BikeSetup(bikeNumber = BIKE_NUMBER, location = location, target = target, availability = availability)

    private data class StandScenario(
        val trackerDriver: String = NAVTELECOM_DRIVER,
        val trackerImei: String = IMEI,
        val trackerStatus: HttpStatusCode = HttpStatusCode.OK,
        val bikeStatus: HttpStatusCode = HttpStatusCode.OK,
        val isTrackerEmulated: Boolean = true,
        val isConnected: Boolean = true,
        val connectionLostAt: String? = null,
        val isGpsLost: Boolean = false,
        val gpsLostSince: String? = null,
        val disabledReason: String? = null,
        val batteryCategory: String? = null,
        val driverTaskStatus: HttpStatusCode = HttpStatusCode.NotFound,
        val availabilityStatus: HttpStatusCode = HttpStatusCode.OK,
        val isIgnoringAvailability: Boolean = false,
        val model: String? = null,
        val disabledReasonTags: List<String> = emptyList(),
        val initialMillivolts: Int = INITIAL_MILLIVOLTS,
        val stalePercent: Double? = null,
        val emulatorChargeReportDelay: Duration = Duration.ZERO,
    )

    private data class AvailabilityRequest(val path: String, val vendors: String?, val body: String)

    private class FakeStand(private val testDispatcher: TestDispatcher, private val scenario: StandScenario) {
        var lastMillivolts = scenario.initialMillivolts
        val simulatorPosts = mutableListOf<Parameters>()
        val availabilityRequests = mutableListOf<AvailabilityRequest>()
        val omniPosts = mutableListOf<String>()
        val voiDevicePosts = mutableListOf<String>()
        val voiCoordinatePosts = mutableListOf<String>()
        private var emulatorPercent: Double? = null
        private var emulatorPercentSentAtMs = 0L
        private var voiDevice = VOI_DEVICE_JSON
        private var disabledReason = scenario.disabledReason
        private var disabledReasonTags = scenario.disabledReasonTags

        private val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = testDispatcher
                addHandler { request ->
                    val path = request.url.encodedPath
                    when {
                        path == SIMULATOR_STATUS_PATH && request.method == HttpMethod.Post -> {
                            simulatorPosts.add(request.url.parameters)
                            val voltage = request.url.parameters["vol"]
                            if (voltage != null) lastMillivolts = voltage.toInt()
                            respond(SIMULATOR_STATUS_JSON, HttpStatusCode.OK, textHeaders)
                        }
                        path == SIMULATOR_STATUS_PATH -> respond(SIMULATOR_STATUS_JSON, HttpStatusCode.OK, textHeaders)
                        path == "/trackers/api/trackers/tracker/${scenario.trackerImei}" && scenario.trackerStatus != HttpStatusCode.OK ->
                            respondError(scenario.trackerStatus)
                        path == "/trackers/api/trackers/tracker/${scenario.trackerImei}" -> respond(trackerJson(), HttpStatusCode.OK, jsonHeaders)
                        path == "/bikes/api/bikes/identifier/S.$BIKE_NUMBER" && scenario.bikeStatus != HttpStatusCode.OK ->
                            respondError(scenario.bikeStatus)
                        path == "/bikes/api/bikes/identifier/S.$BIKE_NUMBER" -> respond(bikeJson(), HttpStatusCode.OK, jsonHeaders)
                        path == "$OMNI_TRACKERS_PATH/${scenario.trackerImei}" && request.method == HttpMethod.Post -> {
                            val body = request.body.toByteArray().decodeToString()
                            omniPosts.add(body)
                            emulatorPercent = numberField(body, "chargePercentage")
                            emulatorPercentSentAtMs = testDispatcher.scheduler.currentTime
                            respond("", HttpStatusCode.OK, jsonHeaders)
                        }
                        path == "$OMNI_TRACKERS_PATH/${scenario.trackerImei}" && scenario.isTrackerEmulated ->
                            respond(OMNI_TRACKER_JSON, HttpStatusCode.OK, jsonHeaders)
                        path == "$OMNI_TRACKERS_PATH/${scenario.trackerImei}" -> respondError(HttpStatusCode.NotFound)
                        path == "$VOI_EMULATOR_PATH/coordinates/${scenario.trackerImei}" -> {
                            voiCoordinatePosts.add(request.body.toByteArray().decodeToString())
                            respond("", HttpStatusCode.OK, jsonHeaders)
                        }
                        path == "$VOI_EMULATOR_PATH/${scenario.trackerImei}" && request.method == HttpMethod.Post -> {
                            val body = request.body.toByteArray().decodeToString()
                            voiDevicePosts.add(body)
                            voiDevice = body
                            emulatorPercent = numberField(body, "batteryPercent")
                            emulatorPercentSentAtMs = testDispatcher.scheduler.currentTime
                            respond("", HttpStatusCode.OK, jsonHeaders)
                        }
                        path == "$VOI_EMULATOR_PATH/${scenario.trackerImei}" && scenario.isTrackerEmulated ->
                            respond(voiDevice, HttpStatusCode.OK, jsonHeaders)
                        path == "$VOI_EMULATOR_PATH/${scenario.trackerImei}" ->
                            respond("not found ${scenario.trackerImei}", HttpStatusCode.BadRequest, textHeaders)
                        path == "/drivernavtelecom/api/trackers/tracker/$IMEI" ->
                            respond(fixture("/tracker-driver.json"), HttpStatusCode.OK, jsonHeaders)
                        path == "/gateway-adm/api/bikes/models/$MODEL_ID" ->
                            respond(scenario.model ?: fixture("/model.json"), HttpStatusCode.OK, jsonHeaders)
                        path == "/gateway-ocelot/api/v1/pickupdropoff-api/tasks/byTransportId/S.$BIKE_NUMBER" &&
                            scenario.driverTaskStatus == HttpStatusCode.OK ->
                            respond(fixture("/driver-task.json"), HttpStatusCode.OK, jsonHeaders)
                        path == "/gateway-ocelot/api/v1/pickupdropoff-api/tasks/byTransportId/S.$BIKE_NUMBER" ->
                            respondError(scenario.driverTaskStatus)
                        path == "/bikes/api/disabledreasons/allowed" ->
                            respond(fixture("/disabled-reasons.json"), HttpStatusCode.OK, jsonHeaders)
                        path == DISABLE_REASON_PATH || path == ENABLE_PATH -> {
                            val body = request.body.toByteArray().decodeToString()
                            availabilityRequests.add(AvailabilityRequest(path, request.headers[SELECTED_VENDORS_HEADER], body))
                            if (scenario.availabilityStatus != HttpStatusCode.OK) {
                                respondError(scenario.availabilityStatus)
                            } else {
                                if (!scenario.isIgnoringAvailability) {
                                    disabledReason = if (path == ENABLE_PATH) null else reasonField(body, REASON_TITLE_FIELD)
                                    disabledReasonTags = if (path == ENABLE_PATH) emptyList() else listOf(reasonField(body, REASON_TAG_FIELD))
                                }
                                respond("""{"errors":[],"succeeded":true}""", HttpStatusCode.OK, jsonHeaders)
                            }
                        }
                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            },
        )

        fun interactor(): BikeSetupInteractor = TestStandInteractors { engine }.bikeSetupInteractor

        private fun trackerJson(): String = """
            {
              "identifier": "S.$BIKE_NUMBER",
              "driver": "${scenario.trackerDriver}",
              "location": {"lat": 43.424838, "lng": 39.928128},
              "telemetry": {"telemetry": {"mainBatteryVoltage": ${lastMillivolts / 1000.0}, "mainBatteryPercent": ${reportedPercent()}}}
            }
        """.trimIndent()

        private fun reportedPercent(): Double? {
            val sinceSentMs = testDispatcher.scheduler.currentTime - emulatorPercentSentAtMs
            return if (sinceSentMs >= scenario.emulatorChargeReportDelay.inWholeMilliseconds) emulatorPercent else null
        }

        private fun bikeJson(): String {
            val voltagePercent = ((lastMillivolts / 1000.0 - MIN_VOLTS) / (MAX_VOLTS - MIN_VOLTS) * 100).coerceIn(0.0, 100.0)
            val percent = scenario.stalePercent ?: reportedPercent() ?: voltagePercent
            val status = when {
                percent <= 0.0 -> "Empty"
                percent <= 10.0 -> "ChargeIsExtremelyLow"
                percent <= 15.0 -> "ChargeIsLow"
                percent <= 25.0 -> "ChargeRecommendedToChange"
                else -> "Ok"
            }
            val battery = if (scenario.batteryCategory == null) {
                "null"
            } else {
                """{"bikeBatteryModel": {"batteryCapacityCategory": "${scenario.batteryCategory}"}}"""
            }
            return """
                {
                  "id": "$BIKE_ID",
                  "identifier": "S.$BIKE_NUMBER",
                  "vendorId": "$VENDOR_ID",
                  "trackerId": "${scenario.trackerImei}",
                  "model": {"id": "$MODEL_ID", "name": "Ninebot L 60L"},
                  "bikeCity": {"cityId": "5f9b2468f0e5ce0001b638e9", "cityName": "Адлер"},
                  "charge": {"batteryPercent": ${percent / 100}, "status": "$status"},
                  "state": {
                    "state": {
                      "connected": ${scenario.isConnected},
                      "connectionLostDate": ${jsonString(scenario.connectionLostAt)},
                      "nullableCoordinates": ${scenario.isGpsLost},
                      "nullableCoordinatesDateTime": ${jsonString(scenario.gpsLostSince)},
                      "disabled": ${disabledReason != null}
                    },
                    "mainDisabledReason": ${jsonString(disabledReason)},
                    "disabledReasonTags": [${disabledReasonTags.joinToString { jsonString(it) }}]
                  },
                  "bikeBatteryInternal": $battery
                }
            """.trimIndent()
        }

        private fun jsonString(value: String?): String = if (value == null) "null" else "\"$value\""

        private fun numberField(body: String, field: String): Double =
            (Json.parseToJsonElement(body) as JsonObject).getValue(field).jsonPrimitive.content.toDouble()

        private fun reasonField(body: String, field: String): String {
            val reasonId = (Json.parseToJsonElement(body) as JsonObject).getValue("reasonId").jsonPrimitive.content
            val reasons = (Json.parseToJsonElement(fixture("/disabled-reasons.json")) as JsonObject).getValue("entries").jsonArray
            val reason = reasons.single { (it as JsonObject).getValue("id").jsonPrimitive.content == reasonId } as JsonObject
            return reason.getValue(field).jsonPrimitive.content
        }

        private fun fixture(path: String): String =
            checkNotNull(javaClass.getResource(path)) { "Нет фикстуры $path" }.readText()
    }

    private companion object {
        const val BASE_URL = TestStandInteractors.BASE_URL
        const val BIKE_NUMBER = 4071
        const val IMEI = "300000000004071"
        const val BIKE_ID = "605b34168c1efdb51cfed7a4"
        const val VENDOR_ID = "URB"
        const val DISABLE_REASON_PATH = "/bikes/api/status/disable-reason/set"
        const val ENABLE_PATH = "/bikes/api/status/enable"
        const val SELECTED_VENDORS_HEADER = "X-Selected-Vendors"
        const val MODEL_ID = "6065d2f5ef64db1143f1dec4"
        const val NEEDS_REPAIR_REASON_ID = "62f35c88e235ee1bcbdcc2a7"
        const val NAVTELECOM_DRIVER = "driver.scooter.tracker.navtelecom.api"
        const val OMNI_DRIVER = "driver.scooter.omni.api"
        const val VOI_DRIVER = "driver.scooter.voi.api"
        const val LIME_DRIVER = "driver.scooter.lime.api"
        const val OMNI_IMEI = "666000000000026"
        const val VOI_IMEI = "867000000000005"
        const val REAL_NAVTELECOM_IMEI = "869000000004071"
        const val OMNI_TRACKERS_PATH = "/emulatoromni/api/trackers"
        const val VOI_EMULATOR_PATH = "/emulatorvoi/api/emulator"
        const val OMNI_TRACKER_JSON = """{"trackerMovementDepsLock":"Never","isConnected":true,"chargePercentage":99,"forcedCoordinate":"","isScooterLocked":true}"""
        const val VOI_DEVICE_JSON = """{"imei":"867000000000005","iccid":"8900000","lat":43.44815,"lng":39.907837,"locked":true,"batteryPercent":85,"signal":32,"lockSendIntervalMSec":60000,"unlockSendIntervalMSec":3000,"lockDelay":5,"isConnected":true,"isUpdateCoordinate":false}"""
        val OMNI_OBSERVED_CHARGE_DELAY = 107.seconds
        val VOI_OBSERVED_CHARGE_DELAY = 101.seconds
        const val LOW_WHOLE_PERCENT = 13.0
        const val NEED_TO_CHANGE_WHOLE_PERCENT = 20
        const val SIMULATOR_STATUS_PATH = "/simulator-signal/api/status"
        const val SIMULATOR_STATUS_JSON = """{"ID":4071,"Lat":"43.42485","Lon":"39.92816","I1":false,"Startid":4000,"Countid":110,"Ping":60}"""
        const val TWO_CATEGORY_MODEL_JSON = """
            {
              "name": "Ninebot SL 90",
              "chargeThresholds": [
                {"batteryCapacityCategory": "Normal", "batteryLevelToEmpty": 0, "batteryLevelToPoweroff": 10,
                 "batteryLevelToDisable": 15, "batteryLevelNeedToChange": 16},
                {"batteryCapacityCategory": "Increased", "batteryLevelToEmpty": 0, "batteryLevelToPoweroff": 6,
                 "batteryLevelToDisable": 11, "batteryLevelNeedToChange": 15}
              ]
            }
        """
        const val INITIAL_MILLIVOLTS = 32000
        const val DISABLED_REASONS_ON_STAND = 30
        const val NORMAL_MILLIVOLTS = 40000
        const val STALE_PERCENT = 62.5
        const val LOW_TARGET_PERCENT = 12.5
        const val LOW_MILLIVOLTS = 34500
        const val LOW_BATTERY_TITLE = "Низкий заряд батареи"
        const val READY_FOR_USE_TAG = "ReadyForUse"
        const val READY_FOR_USE_TITLE = "Готов к эксплуатации"
        const val READY_FOR_USE_REASON_ID = "6130ef586c784359f750ef23"
        const val LOW_BATTERY_TAG = "LowBattery"
        const val REASON_TITLE_FIELD = "description"
        const val REASON_TAG_FIELD = "name"
        const val MIN_VOLTS = 33.4
        const val MAX_VOLTS = 41.5
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val textHeaders = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
    }
}
