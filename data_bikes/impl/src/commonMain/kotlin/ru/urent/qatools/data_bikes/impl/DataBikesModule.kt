package ru.urent.qatools.data_bikes.impl

import org.koin.dsl.module
import ru.urent.qatools.data_bikes.BikesRepository

class DataBikesModule {
    val module = module {
        single<BikesRepository> { BikesRepositoryImpl(clientProvider = get()) }
    }
}
