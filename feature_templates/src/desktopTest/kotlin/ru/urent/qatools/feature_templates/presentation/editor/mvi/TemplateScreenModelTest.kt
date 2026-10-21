package ru.urent.qatools.feature_templates.presentation.editor.mvi

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
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.testing_stand.ScreenTestStand
import ru.urent.qatools.testing_stand.SilentLog

class TemplateScreenModelTest {

    @Test
    fun bikesRejectedWithoutVpnAreLoadedAgainAfterReconnect() = runBlocking<Unit> {
        val stand = ScreenTestStand(rejectedBikeRequests = 1)
        val templateId = createTemplate(stand)
        val reconnections = MutableSharedFlow<Unit>()
        val screenModel = screenModel(stand, templateId, reconnections)
        withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { state -> state.rows.singleOrNull()?.lookup is BikeLookup.Failed } }

        reconnections.emit(Unit)

        withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { state -> state.rows.singleOrNull()?.lookup is BikeLookup.Found } }
    }

    @Test
    fun choosingTechZoneMovesBikeToItsCenterAndSelectsIt() = runBlocking<Unit> {
        val stand = ScreenTestStand()
        val templateId = createTemplate(stand)
        val screenModel = screenModel(stand, templateId, emptyFlow())
        val bike = withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.rows.isNotEmpty() } }.rows.single().bike

        screenModel.dispatch(TemplateEvent.OnBikeTechZoneSelected(bikeId = bike.id, techZone = VykhinoTechZone))

        withTimeout(WAIT_TIMEOUT_MS) {
            stand.interactors.templateInteractor.getTemplate(templateId).first { it?.bikes?.single()?.setup?.location == VykhinoTechZone.center }
        }
        assertEquals(bike.id, screenModel.stateFlow.value.selectedBikeId)
        assertEquals(VykhinoTechZone.center, screenModel.stateFlow.value.focus?.point)
    }

    private suspend fun createTemplate(stand: ScreenTestStand): Long {
        val templateInteractor = stand.interactors.templateInteractor
        val templateId = templateInteractor.create(standBaseUrl = ScreenTestStand.BASE_URL, name = "Тестовый шаблон")
        val setup = BikeSetup(
            simulatorId = ScreenTestStand.FOUND_SIMULATOR_ID,
            location = GeoPoint(latitude = 43.424838, longitude = 39.928128),
            target = ChargeTarget.State(ChargeState.NORMAL),
            availability = AvailabilityTarget.Keep,
        )
        templateInteractor.addBike(templateId = templateId, setup = setup)
        return templateId
    }

    private fun screenModel(stand: ScreenTestStand, templateId: Long, reconnections: Flow<Unit>) = TemplateScreenModel(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        standBaseUrl = ScreenTestStand.BASE_URL,
        templateId = templateId,
        templateInteractor = stand.interactors.templateInteractor,
        locationInteractor = stand.interactors.locationInteractor,
        bikeSetupInteractor = stand.interactors.bikeSetupInteractor,
        runner = stand.interactors.runner,
        tileSource = ScreenTestStand.Tiles,
        reconnections = reconnections,
        log = SilentLog,
    )

    private companion object {
        const val WAIT_TIMEOUT_MS = 5_000L
        val VykhinoTechZone = TechZone(
            id = "vykhino",
            name = "Москва Выхино-Жулебино",
            center = GeoPoint(latitude = 55.7067, longitude = 37.8183),
            outline = emptyList(),
        )
    }
}
