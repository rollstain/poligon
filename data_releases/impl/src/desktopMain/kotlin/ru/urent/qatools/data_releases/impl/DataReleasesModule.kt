package ru.urent.qatools.data_releases.impl

import org.koin.dsl.module
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.data_releases.ReleasesRepository

class DataReleasesModule {
    val module = module {
        single<ReleasesRepository> { ReleasesRepositoryImpl(httpClient = get<HttpClientFactory>().createPlain()) }
    }
}
