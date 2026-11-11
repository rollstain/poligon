package ru.urent.qatools.base_bike_setup.domain

import kotlin.time.Duration
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import ru.urent.qatools.base_bike_setup.domain.mappers.BikeMapper
import ru.urent.qatools.base_bike_setup.entities.ApplyFailure
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.BikeStatus
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.base_bike_setup.entities.TrackerEmulator
import ru.urent.qatools.base_bike_setup.entities.VoltageRange
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestFailure
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.data_bikes.BikesRepository
import ru.urent.qatools.data_bikes.response.TrackerResponse
import ru.urent.qatools.data_simulator.OmniEmulatorRepository
import ru.urent.qatools.data_simulator.SimulatorRepository
import ru.urent.qatools.data_simulator.VoiEmulatorRepository

class BikeSetupInteractor(
    private val bikesRepository: BikesRepository,
    private val simulatorRepository: SimulatorRepository,
    private val omniEmulatorRepository: OmniEmulatorRepository,
    private val voiEmulatorRepository: VoiEmulatorRepository,
    private val bikeMapper: BikeMapper,
    private val log: AppLog,
) {

    suspend fun lookup(standBaseUrl: String, bikeNumber: Int): BikeLookup {
        val identifier = IDENTIFIER_PREFIX + bikeNumber
        val bike = when (val loaded = bikesRepository.getBike(identifier)) {
            is RequestResult.Error -> return BikeLookup.Failed(notFoundOr(loaded.failure, ApplyFailure.BikeNotFound))
            is RequestResult.Success -> loaded.data
        }
        val bikeId = bike.id ?: return BikeLookup.Failed(ApplyFailure.BikeNotFound)
        val modelId = bike.model?.id ?: return BikeLookup.Failed(ApplyFailure.NoChargeThresholds)
        val imei = bike.trackerImei
        if (imei.isNullOrBlank()) return BikeLookup.Failed(ApplyFailure.TrackerNotLinked)
        val tracker = when (val loaded = bikesRepository.getTracker(imei)) {
            is RequestResult.Error -> return BikeLookup.Failed(notFoundOr(loaded.failure, ApplyFailure.TrackerNotFound))
            is RequestResult.Success -> loaded.data
        }
        val driver = tracker.driver.orEmpty()
        val emulator = when (driver) {
            NAVTELECOM_DRIVER -> {
                val simulatorImei = IMEI_PREFIX + bikeNumber.toString().padStart(IMEI_NUMBER_LENGTH, IMEI_PAD_CHAR)
                if (imei != simulatorImei) return BikeLookup.Failed(ApplyFailure.TrackerNotEmulated(imei))
                val settings = when (val loaded = bikesRepository.getTrackerSettings(imei)) {
                    is RequestResult.Error -> return BikeLookup.Failed(applyFailureOf(loaded.failure))
                    is RequestResult.Success -> loaded.data
                }
                val minVolts = settings.minVoltageOneBattery
                val maxVolts = settings.maxVoltageOneBattery
                if (minVolts == null || maxVolts == null || maxVolts <= minVolts) return BikeLookup.Failed(ApplyFailure.NoVoltageRange)
                TrackerEmulator.NavtelecomSimulator(simulatorId = bikeNumber, voltageRange = VoltageRange(minVolts = minVolts, maxVolts = maxVolts))
            }
            OMNI_DRIVER -> {
                val isEmulated = when (val loaded = omniEmulatorRepository.isTrackerEmulated(baseUrl = standBaseUrl, imei = imei)) {
                    is RequestResult.Error -> return BikeLookup.Failed(applyFailureOf(loaded.failure))
                    is RequestResult.Success -> loaded.data
                }
                if (!isEmulated) return BikeLookup.Failed(ApplyFailure.TrackerNotEmulated(imei))
                TrackerEmulator.Omni
            }
            VOI_DRIVER -> {
                val device = when (val loaded = voiEmulatorRepository.getDevice(baseUrl = standBaseUrl, imei = imei)) {
                    is RequestResult.Error -> return BikeLookup.Failed(applyFailureOf(loaded.failure))
                    is RequestResult.Success -> loaded.data
                }
                if (device == null) return BikeLookup.Failed(ApplyFailure.TrackerNotEmulated(imei))
                val reportIntervalMs = maxOf(device.lockedReportIntervalMs ?: 0, device.unlockedReportIntervalMs ?: 0)
                TrackerEmulator.Voi(reportInterval = reportIntervalMs.milliseconds)
            }
            else -> return BikeLookup.Failed(ApplyFailure.UnsupportedDriver(driver))
        }

        val model = when (val loaded = bikesRepository.getTransportModel(modelId)) {
            is RequestResult.Error -> return BikeLookup.Failed(applyFailureOf(loaded.failure))
            is RequestResult.Success -> loaded.data
        }
        val batteryCategory = bike.internalBattery?.model?.capacityCategory ?: BatteryCapacityCategory.NOT_SET
        val thresholdCategory = if (batteryCategory == BatteryCapacityCategory.INCREASED) {
            BatteryCapacityCategory.INCREASED
        } else {
            BatteryCapacityCategory.NORMAL
        }
        val modelThresholds = model.chargeThresholds.orEmpty()
        val threshold = modelThresholds.firstOrNull { it?.batteryCapacityCategory == thresholdCategory }
            ?: modelThresholds.firstOrNull { it?.batteryCapacityCategory == BatteryCapacityCategory.NORMAL }
        val empty = threshold?.batteryLevelToEmpty
        val powerOff = threshold?.batteryLevelToPoweroff
        val disable = threshold?.batteryLevelToDisable
        val needToChange = threshold?.batteryLevelNeedToChange
        if (empty == null || powerOff == null || disable == null || needToChange == null) {
            return BikeLookup.Failed(ApplyFailure.NoChargeThresholds)
        }

        val trackerLatitude = tracker.location?.lat
        val trackerLongitude = tracker.location?.lng
        val location = if (trackerLatitude == null || trackerLongitude == null) {
            null
        } else {
            GeoPoint(latitude = trackerLatitude, longitude = trackerLongitude)
        }
        val batteryFraction = bike.charge?.batteryFraction
        val flags = bike.state?.flags
        val status = BikeStatus(
            isOnline = flags?.isConnected,
            offlineSince = parseInstant(flags?.connectionLostAt),
            isGpsLost = flags?.hasNoCoordinates == true,
            gpsLostSince = parseInstant(flags?.noCoordinatesSince),
            isDisabled = flags?.isDisabled,
            disabledReason = bike.state?.mainDisabledReason,
            hasLowBatteryReason = bike.state?.disabledReasonTags.orEmpty().contains(LOW_BATTERY_REASON_NAME),
            batteryCategory = batteryCategory,
            driverTask = driverTaskOf(identifier),
        )
        val context = BikeContext(
            bikeNumber = bikeNumber,
            imei = imei,
            bikeId = bikeId,
            vendorId = bike.vendorId.orEmpty(),
            identifier = identifier,
            modelName = model.name ?: bike.model?.name.orEmpty(),
            cityId = bike.bikeCity?.cityId,
            cityName = bike.bikeCity?.cityName,
            chargePercent = if (batteryFraction == null) null else batteryFraction * PERCENT_IN_FRACTION,
            chargeState = bike.charge?.status,
            location = location,
            emulator = emulator,
            thresholds = ChargeThresholds(
                emptyPercent = empty,
                powerOffPercent = powerOff,
                disablePercent = disable,
                needToChangePercent = needToChange,
            ),
            disableStates = model.disableStates.orEmpty().toSet(),
            status = status,
        )
        return BikeLookup.Found(context)
    }

    suspend fun getDisabledReasons(): List<DisabledReason> {
        val loaded = bikesRepository.getDisabledReasons() as? RequestResult.Success ?: return emptyList()
        return bikeMapper.toDisabledReasons(loaded.data)
    }

    fun apply(standBaseUrl: String, setup: BikeSetup): Flow<ApplyStatus> = flow {
        emit(ApplyStatus.Resolving)
        val bike = when (val lookup = lookup(standBaseUrl = standBaseUrl, bikeNumber = setup.bikeNumber)) {
            is BikeLookup.Failed -> {
                emit(ApplyStatus.Failed(lookup.failure))
                return@flow
            }
            is BikeLookup.Found -> lookup.bike
        }
        val percent = when (bike.emulator) {
            is TrackerEmulator.NavtelecomSimulator -> ChargeCalculator.targetPercent(setup.target, bike.thresholds)
            TrackerEmulator.Omni, is TrackerEmulator.Voi -> ChargeCalculator.wholePercent(setup.target, bike.thresholds).toDouble()
        }
        val sendFailure = send(standBaseUrl = standBaseUrl, bike = bike, setup = setup)
        if (sendFailure != null) {
            emit(ApplyStatus.Failed(sendFailure))
            return@flow
        }
        emit(ApplyStatus.WaitingForTracker(percent))
        val confirmation = confirm(standBaseUrl = standBaseUrl, bike = bike, setup = setup, percent = percent)
        if (confirmation !is ApplyStatus.Done) {
            emit(confirmation)
            return@flow
        }
        val availabilityFailure = changeAvailability(bike = bike, target = setup.availability)
        emit(if (availabilityFailure == null) confirmation else ApplyStatus.Failed(availabilityFailure))
    }.onEach { status -> log.info("S.${setup.bikeNumber}: $status") }

    suspend fun confirmRecalculatedCharge(setup: BikeSetup, applied: ApplyStatus.Done, bike: BikeContext): ApplyStatus {
        val target = setup.target
        if (target !is ChargeTarget.State || bike.chargeState != target.state) return applied
        val availability = setup.availability
        val isLowBatteryReasonWanted = availability is AvailabilityTarget.Disabled && availability.reasonName == LOW_BATTERY_REASON_NAME
        val isLowBatteryReasonLeftByStand = bike.status.hasLowBatteryReason &&
            availability != AvailabilityTarget.Keep &&
            !isLowBatteryReasonWanted &&
            target.state !in bike.disableStates
        val availabilityFailure = if (isLowBatteryReasonLeftByStand) changeAvailability(bike = bike, target = availability) else null
        val result = if (availabilityFailure == null) {
            applied.copy(state = bike.chargeState, isTargetReached = true)
        } else {
            ApplyStatus.Failed(availabilityFailure)
        }
        val repeatedAvailabilityText = if (isLowBatteryReasonLeftByStand) ", доступ выставлен повторно вместо «Низкий заряд батареи»" else ""
        log.info("S.${setup.bikeNumber}: стенд пересчитал заряд$repeatedAvailabilityText: $result")
        return result
    }

    fun applyAll(standBaseUrl: String, bikes: List<TemplateBike>): Flow<Pair<Long, ApplyStatus>> = channelFlow {
        val semaphore = Semaphore(MAX_PARALLEL_BIKES)
        for (bike in bikes) {
            launch {
                semaphore.withPermit {
                    apply(standBaseUrl = standBaseUrl, setup = bike.setup).collect { status -> send(bike.id to status) }
                }
            }
        }
    }

    private suspend fun driverTaskOf(identifier: String): DriverTask? {
        val task = bikesRepository.getDriverTask(identifier) as? RequestResult.Success ?: return null
        return DriverTask(reason = task.data.reason)
    }

    private fun parseInstant(text: String?): Instant? =
        if (text == null) null else runCatching { Instant.parse(text) }.getOrNull()

    private suspend fun changeAvailability(bike: BikeContext, target: AvailabilityTarget): ApplyFailure? {
        val changed = when (target) {
            AvailabilityTarget.Keep -> return null
            AvailabilityTarget.Available -> bikesRepository.enable(bikeId = bike.bikeId, vendorId = bike.vendorId)
            is AvailabilityTarget.Disabled -> {
                val reasons = when (val loaded = bikesRepository.getDisabledReasons()) {
                    is RequestResult.Error -> return ApplyFailure.AvailabilityNotChanged(applyFailureOf(loaded.failure))
                    is RequestResult.Success -> bikeMapper.toDisabledReasons(loaded.data)
                }
                val reason = reasons.firstOrNull { it.name == target.reasonName } ?: return ApplyFailure.ReasonNotFound(target.reasonTitle)
                bikesRepository.replaceDisabledReason(
                    bikeId = bike.bikeId,
                    vendorId = bike.vendorId,
                    reasonId = reason.id,
                    description = DISABLE_DESCRIPTION,
                )
            }
        }
        if (changed is RequestResult.Error) return ApplyFailure.AvailabilityNotChanged(applyFailureOf(changed.failure))
        return failureIfNotApplied(identifier = bike.identifier, target = target)
    }

    private suspend fun failureIfNotApplied(identifier: String, target: AvailabilityTarget): ApplyFailure? {
        val state = when (val loaded = bikesRepository.getBike(identifier)) {
            is RequestResult.Error -> return ApplyFailure.AvailabilityNotChanged(applyFailureOf(loaded.failure))
            is RequestResult.Success -> loaded.data.state
        }
        val isDisabled = state?.flags?.isDisabled == true
        val currentReason = if (isDisabled) state?.mainDisabledReason else null
        val isApplied = when (target) {
            AvailabilityTarget.Keep -> true
            AvailabilityTarget.Available -> !isDisabled
            is AvailabilityTarget.Disabled -> currentReason == target.reasonTitle
        }
        return if (isApplied) null else ApplyFailure.AvailabilityIgnored(isDisabled = isDisabled, currentReason = currentReason)
    }

    private suspend fun send(standBaseUrl: String, bike: BikeContext, setup: BikeSetup): ApplyFailure? {
        val sent = when (val emulator = bike.emulator) {
            is TrackerEmulator.NavtelecomSimulator -> {
                val located = simulatorRepository.setLocation(baseUrl = standBaseUrl, simulatorId = emulator.simulatorId, location = setup.location)
                if (located is RequestResult.Error) return applyFailureOf(located.failure)
                val millivolts = ChargeCalculator.millivolts(setup.target, bike.thresholds, emulator.voltageRange)
                simulatorRepository.setVoltage(baseUrl = standBaseUrl, simulatorId = emulator.simulatorId, millivolts = millivolts)
            }
            TrackerEmulator.Omni -> omniEmulatorRepository.setChargeAndLocation(
                baseUrl = standBaseUrl,
                imei = bike.imei,
                chargePercent = ChargeCalculator.wholePercent(setup.target, bike.thresholds),
                location = setup.location,
            )
            is TrackerEmulator.Voi -> {
                val located = voiEmulatorRepository.setLocation(baseUrl = standBaseUrl, imei = bike.imei, location = setup.location)
                if (located is RequestResult.Error) return applyFailureOf(located.failure)
                voiEmulatorRepository.setBatteryPercent(
                    baseUrl = standBaseUrl,
                    imei = bike.imei,
                    batteryPercent = ChargeCalculator.wholePercent(setup.target, bike.thresholds),
                )
            }
        }
        return if (sent is RequestResult.Error) applyFailureOf(sent.failure) else null
    }

    private suspend fun confirm(standBaseUrl: String, bike: BikeContext, setup: BikeSetup, percent: Double): ApplyStatus {
        val waited = when (val emulator = bike.emulator) {
            is TrackerEmulator.NavtelecomSimulator -> {
                val simulator = when (val loaded = simulatorRepository.getStatus(baseUrl = standBaseUrl, simulatorId = emulator.simulatorId)) {
                    is RequestResult.Error -> return ApplyStatus.Failed(applyFailureOf(loaded.failure))
                    is RequestResult.Success -> loaded.data
                }
                val pingSeconds = simulator.pingSeconds ?: return ApplyStatus.Failed(ApplyFailure.Unexpected(null))
                val millivolts = ChargeCalculator.millivolts(setup.target, bike.thresholds, emulator.voltageRange)
                awaitTelemetry(imei = bike.imei, timeout = (pingSeconds + REPORT_GRACE_SECONDS).seconds) { telemetry ->
                    val voltage = telemetry.mainBatteryVoltage
                    voltage != null && ChargeCalculator.toMillivolts(voltage) == millivolts
                }
            }
            TrackerEmulator.Omni -> awaitTelemetry(
                imei = bike.imei,
                timeout = OMNI_REPORT_INTERVAL * REPORTS_UNTIL_NEW_CHARGE + REPORT_GRACE_SECONDS.seconds,
            ) { telemetry ->
                telemetry.mainBatteryPercent?.roundToInt() == percent.roundToInt()
            }
            is TrackerEmulator.Voi -> awaitTelemetry(
                imei = bike.imei,
                timeout = emulator.reportInterval * REPORTS_UNTIL_NEW_CHARGE + REPORT_GRACE_SECONDS.seconds,
            ) { telemetry ->
                telemetry.mainBatteryPercent?.roundToInt() == percent.roundToInt()
            }
        }
        val isChargeReported = when (waited) {
            is RequestResult.Error -> return ApplyStatus.Failed(applyFailureOf(waited.failure))
            is RequestResult.Success -> waited.data
        }
        if (!isChargeReported) return ApplyStatus.Failed(ApplyFailure.NotConfirmed)
        val charge = when (val loaded = bikesRepository.getBike(bike.identifier)) {
            is RequestResult.Error -> return ApplyStatus.Failed(applyFailureOf(loaded.failure))
            is RequestResult.Success -> loaded.data.charge
        }
        val target = setup.target
        return ApplyStatus.Done(
            percent = percent,
            state = charge?.status,
            isTargetReached = target !is ChargeTarget.State || charge?.status == target.state,
            availability = setup.availability,
        )
    }

    private suspend fun awaitTelemetry(
        imei: String,
        timeout: Duration,
        isReported: (TrackerResponse.Values) -> Boolean,
    ): RequestResult<Boolean> {
        val reported = withTimeoutOrNull(timeout) {
            var check = isTelemetryReported(imei = imei, isReported = isReported)
            while (check is RequestResult.Success && !check.data) {
                delay(POLL_INTERVAL)
                check = isTelemetryReported(imei = imei, isReported = isReported)
            }
            check
        }
        return reported ?: RequestResult.Success(false)
    }

    private suspend fun isTelemetryReported(imei: String, isReported: (TrackerResponse.Values) -> Boolean): RequestResult<Boolean> =
        when (val loaded = bikesRepository.getTracker(imei)) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val telemetry = loaded.data.telemetry?.values
                RequestResult.Success(telemetry != null && isReported(telemetry))
            }
        }

    private fun notFoundOr(failure: RequestFailure, notFound: ApplyFailure): ApplyFailure {
        val isNotFound = failure is RequestFailure.Http && failure.statusCode in notFoundStatusCodes
        return if (isNotFound) notFound else applyFailureOf(failure)
    }

    private fun applyFailureOf(failure: RequestFailure): ApplyFailure = when (failure) {
        RequestFailure.NoConnection -> ApplyFailure.StandUnavailable
        RequestFailure.SessionExpired, RequestFailure.InvalidCredentials -> ApplyFailure.SessionExpired
        RequestFailure.InvalidResponse -> ApplyFailure.Unexpected(null)
        is RequestFailure.Http -> ApplyFailure.Unexpected(failure.statusCode)
    }

    private companion object {
        const val IDENTIFIER_PREFIX = "S."
        const val IMEI_PREFIX = "3"
        const val IMEI_NUMBER_LENGTH = 14
        const val IMEI_PAD_CHAR = '0'
        const val NAVTELECOM_DRIVER = "driver.scooter.tracker.navtelecom.api"
        const val OMNI_DRIVER = "driver.scooter.omni.api"
        const val VOI_DRIVER = "driver.scooter.voi.api"
        const val DISABLE_DESCRIPTION = "Полигон"
        const val LOW_BATTERY_REASON_NAME = "LowBattery"
        const val PERCENT_IN_FRACTION = 100.0
        const val REPORT_GRACE_SECONDS = 30
        const val REPORTS_UNTIL_NEW_CHARGE = 2
        const val MAX_PARALLEL_BIKES = 4
        const val BAD_REQUEST_STATUS_CODE = 400
        const val NOT_FOUND_STATUS_CODE = 404
        val POLL_INTERVAL = 5.seconds
        val OMNI_REPORT_INTERVAL = 90.seconds
        val notFoundStatusCodes = setOf(BAD_REQUEST_STATUS_CODE, NOT_FOUND_STATUS_CODE)
    }
}
