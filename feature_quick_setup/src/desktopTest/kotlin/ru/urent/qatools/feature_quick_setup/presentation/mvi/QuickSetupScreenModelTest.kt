package ru.urent.qatools.feature_quick_setup.presentation.mvi

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ru.urent.qatools.base_bike_setup.domain.PlacementCalculator
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.testing_stand.ScreenTestStand
import ru.urent.qatools.testing_stand.SilentLog

class QuickSetupScreenModelTest {

    @Test
    fun parkingOfFoundBikeIsKnownWithoutVisibleMap() = runBlocking {
        val stand = ScreenTestStand()
        val screenModel = screenModel(stand, emptyFlow())

        screenModel.dispatch(QuickSetupEvent.OnSimulatorIdChanged(ScreenTestStand.FOUND_SIMULATOR_ID.toString()))

        val state = withTimeout(WAIT_TIMEOUT_MS) {
            screenModel.stateFlow.first { it.lookup is BikeLookup.Found && it.zones.parkings.isNotEmpty() }
        }
        val placement = PlacementCalculator.placementOf(checkNotNull(state.location) { "Нет места байка" }, state.zones)
        assertEquals(ScreenTestStand.BIKE_PARKING_NAME, placement.parkingName)
    }

    @Test
    fun bikeRejectedWithoutVpnIsLoadedAgainAfterReconnectKeepingPin() = runBlocking {
        val stand = ScreenTestStand(rejectedBikeRequests = 1)
        val reconnections = MutableSharedFlow<Unit>()
        val screenModel = screenModel(stand, reconnections)
        screenModel.dispatch(QuickSetupEvent.OnSimulatorIdChanged(ScreenTestStand.FOUND_SIMULATOR_ID.toString()))
        withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.lookup is BikeLookup.Failed } }
        screenModel.dispatch(QuickSetupEvent.OnCoordinatesTextChanged(PIN_TEXT))

        reconnections.emit(Unit)

        val state = withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.lookup is BikeLookup.Found } }
        assertEquals(PinPlacedByQa, state.location)
    }

    private fun screenModel(stand: ScreenTestStand, reconnections: Flow<Unit>) = QuickSetupScreenModel(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        standBaseUrl = ScreenTestStand.BASE_URL,
        bikeSetupInteractor = stand.interactors.bikeSetupInteractor,
        templateInteractor = stand.interactors.templateInteractor,
        locationInteractor = stand.interactors.locationInteractor,
        tileSource = ScreenTestStand.Tiles,
        reconnections = reconnections,
        log = SilentLog,
    )

    private companion object {
        const val WAIT_TIMEOUT_MS = 5_000L
        const val PIN_TEXT = "43.43, 39.93"
        val PinPlacedByQa = GeoPoint(latitude = 43.43, longitude = 39.93)
    }
}
