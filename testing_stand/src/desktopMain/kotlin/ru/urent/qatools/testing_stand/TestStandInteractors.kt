package ru.urent.qatools.testing_stand

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.base_bike_setup.domain.BikeSetupInteractor
import ru.urent.qatools.base_bike_setup.domain.LocationInteractor
import ru.urent.qatools.base_bike_setup.domain.TemplateApplyRunner
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_bike_setup.domain.mappers.BikeMapper
import ru.urent.qatools.base_bike_setup.domain.mappers.LocationMapper
import ru.urent.qatools.base_bike_setup.domain.mappers.TemplateMapper
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.StandHttpClientProvider
import ru.urent.qatools.core_network.StandRejectionListener
import ru.urent.qatools.core_network.impl.HttpClientFactoryImpl
import ru.urent.qatools.data_bikes.impl.DataBikesModule
import ru.urent.qatools.data_location.impl.DataLocationModule
import ru.urent.qatools.data_simulator.impl.DataSimulatorModule
import ru.urent.qatools.data_templates.impl.DataTemplatesModule

class TestStandInteractors(createEngine: () -> HttpClientEngine) {
    private val clientFactory = HttpClientFactoryImpl(
        engineFactory = TestEngineFactory(createEngine),
        cachingEngineFactory = TestEngineFactory(createEngine),
        log = SilentLog,
    )
    private val standConnection = clientFactory.createForStand(
        baseUrl = BASE_URL,
        accessToken = ACCESS_TOKEN,
        reauthenticate = { null },
        onSessionExpired = {},
        onRejected = {},
    )
    private val koin = koinApplication {
        modules(
            DataBikesModule().module,
            DataLocationModule().module,
            DataSimulatorModule().module,
            DataTemplatesModule().module,
            module {
                single<DatabaseDriverProvider> { InMemoryDatabaseDriverProvider }
                single<StandHttpClientProvider> {
                    object : StandHttpClientProvider {
                        override val httpClient: HttpClient
                            get() = standConnection.client
                    }
                }
                single<HttpClientFactory> { clientFactory }
                single<StandRejectionListener> { StandRejectionListener {} }
            },
        )
    }.koin

    val bikeSetupInteractor = BikeSetupInteractor(
        bikesRepository = koin.get(),
        simulatorRepository = koin.get(),
        bikeMapper = BikeMapper(),
        log = SilentLog,
    )
    val templateInteractor = TemplateInteractor(templateRepository = koin.get(), templateMapper = TemplateMapper())
    val locationInteractor = LocationInteractor(
        locationRepository = koin.get(),
        lastCityRepository = koin.get(),
        locationMapper = LocationMapper(),
    )
    val runner = TemplateApplyRunner(
        bikeSetupInteractor = bikeSetupInteractor,
        templateInteractor = templateInteractor,
        standBaseUrls = flowOf(BASE_URL),
    )

    companion object {
        const val BASE_URL = "https://stand.test"
        private const val ACCESS_TOKEN = "test-token"
    }
}
