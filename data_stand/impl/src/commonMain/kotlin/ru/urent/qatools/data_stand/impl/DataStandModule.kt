package ru.urent.qatools.data_stand.impl

import org.koin.dsl.module
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.data_stand.StandRepository

class DataStandModule {
    val module = module {
        single<StandRepository> {
            StandRepositoryImpl(connection = get<HttpClientFactory>().createStandFacing(onRejected = {}), database = get())
        }
    }
}
