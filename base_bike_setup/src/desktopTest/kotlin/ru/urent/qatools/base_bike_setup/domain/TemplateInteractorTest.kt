package ru.urent.qatools.base_bike_setup.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.base_bike_setup.domain.mappers.TemplateMapper
import ru.urent.qatools.base_bike_setup.entities.ApplyFailure
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.data_templates.impl.DataTemplatesModule
import ru.urent.qatools.testing_stand.InMemoryDatabaseDriverProvider

class TemplateInteractorTest {
    private val interactor = createInteractor()

    @Test
    fun availabilityIsStoredAndChangedWithBike() = runTest {
        val templateId = interactor.create(standBaseUrl = STAND, name = TEMPLATE_NAME)
        val needsRepair = AvailabilityTarget.Disabled(reasonName = "NeedsRepair", reasonTitle = "Требует ремонта")

        interactor.addBike(
            templateId = templateId,
            setup = BikeSetup(
                simulatorId = SIMULATOR_ID,
                location = GeoPoint(43.42, 39.93),
                target = ChargeTarget.State(ChargeState.LOW),
                availability = needsRepair,
            ),
        )
        val added = interactor.getTemplate(templateId).first()?.bikes.orEmpty().single()
        interactor.changeBikeSettings(bikeId = added.id, target = ChargeTarget.Percent(50.0), availability = AvailabilityTarget.Available)
        val changed = interactor.getTemplate(templateId).first()?.bikes.orEmpty().single()

        assertEquals(needsRepair, added.setup.availability)
        assertEquals(AvailabilityTarget.Available, changed.setup.availability)
        assertEquals(ChargeTarget.Percent(50.0), changed.setup.target)
    }

    @Test
    fun applyResultIsReadBackWithAppliedSetupAndNestedFailure() = runTest {
        val bike = addBike()
        val failure = ApplyFailure.AvailabilityNotChanged(ApplyFailure.Unexpected(INTERNAL_ERROR_CODE))
        val result = BikeApplyResult(appliedSetup = bike.setup, status = ApplyStatus.Failed(failure), appliedAt = AppliedAt)

        interactor.saveApplyResult(templateBikeId = bike.id, result = result)

        assertEquals(mapOf(bike.id to result), interactor.getApplyResults(STAND).first())
    }

    @Test
    fun removedBikeLosesItsApplyResult() = runTest {
        val bike = addBike()
        val done = ApplyStatus.Done(percent = 12.5, state = ChargeState.LOW, isTargetReached = true, availability = AvailabilityTarget.Keep)
        interactor.saveApplyResult(templateBikeId = bike.id, result = BikeApplyResult(appliedSetup = bike.setup, status = done, appliedAt = AppliedAt))

        interactor.deleteBike(bike.id)

        assertEquals(emptyMap(), interactor.getApplyResults(STAND).first())
    }

    private suspend fun addBike(): TemplateBike {
        val templateId = interactor.create(standBaseUrl = STAND, name = TEMPLATE_NAME)
        interactor.addBike(
            templateId = templateId,
            setup = BikeSetup(
                simulatorId = SIMULATOR_ID,
                location = GeoPoint(43.42, 39.93),
                target = ChargeTarget.State(ChargeState.LOW),
                availability = AvailabilityTarget.Disabled(reasonName = "NeedsRepair", reasonTitle = "Требует ремонта"),
            ),
        )
        return interactor.getTemplate(templateId).first()?.bikes.orEmpty().single()
    }

    private fun createInteractor(): TemplateInteractor {
        val koin = koinApplication {
            modules(
                DataTemplatesModule().module,
                module { single<DatabaseDriverProvider> { InMemoryDatabaseDriverProvider } },
            )
        }.koin
        return TemplateInteractor(templateRepository = koin.get(), templateMapper = TemplateMapper())
    }

    private companion object {
        const val STAND = "https://tm-0.dev.urent.ru"
        const val TEMPLATE_NAME = "Смоук QA"
        const val SIMULATOR_ID = 4071
        const val INTERNAL_ERROR_CODE = 500
        val AppliedAt = Instant.parse("2026-10-18T00:29:33Z")
    }
}
