package ru.urent.qatools.base_stand

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.StandHttpClientProvider
import ru.urent.qatools.core_network.StandRejectionListener

class BaseStandModule {
    val module = module {
        single {
            val httpClientFactory = get<HttpClientFactory>()
            StandConnectionMonitor(
                createProbeClient = { httpClientFactory.createPlain() },
                scope = CoroutineScope(SupervisorJob()),
                log = get(),
            )
        } bind StandRejectionListener::class
        single {
            StandSessionInteractor(
                repository = get(),
                tokenCipher = get(),
                httpClientFactory = get(),
                connectionMonitor = get(),
                log = get(),
            )
        }
        single<StandHttpClientProvider> { StandSessionHttpClientProvider(sessionInteractor = get()) }
    }
}
