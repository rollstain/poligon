package ru.urent.qatools.base_bike_setup.di

import kotlinx.coroutines.flow.map
import org.koin.dsl.module
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.base_bike_setup.domain.BikeSetupInteractor
import ru.urent.qatools.base_bike_setup.domain.LocationInteractor
import ru.urent.qatools.base_bike_setup.domain.TemplateApplyRunner
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_bike_setup.domain.mappers.BikeMapper
import ru.urent.qatools.base_bike_setup.domain.mappers.LocationMapper
import ru.urent.qatools.base_bike_setup.domain.mappers.TemplateMapper

class BaseBikeSetupModule {
    val module = module {
        factory { BikeMapper() }
        factory { LocationMapper() }
        factory { TemplateMapper() }
        single {
            BikeSetupInteractor(
                bikesRepository = get(),
                simulatorRepository = get(),
                bikeMapper = get(),
                log = get(),
            )
        }
        single { TemplateInteractor(templateRepository = get(), templateMapper = get()) }
        single { LocationInteractor(locationRepository = get(), lastCityRepository = get(), locationMapper = get()) }
        single {
            TemplateApplyRunner(
                bikeSetupInteractor = get(),
                templateInteractor = get(),
                standBaseUrls = get<StandSessionInteractor>().session.map { session -> session?.baseUrl },
            )
        }
    }
}
