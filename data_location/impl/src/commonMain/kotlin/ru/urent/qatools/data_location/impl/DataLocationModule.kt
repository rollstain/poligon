package ru.urent.qatools.data_location.impl

import org.koin.dsl.module
import ru.urent.qatools.data_location.LocationRepository

class DataLocationModule {
    val module = module {
        single<LocationRepository> { LocationRepositoryImpl(clientProvider = get()) }
    }
}
