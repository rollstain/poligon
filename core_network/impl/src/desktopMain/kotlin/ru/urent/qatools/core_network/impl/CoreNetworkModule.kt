package ru.urent.qatools.core_network.impl

import io.ktor.client.engine.config
import io.ktor.client.engine.java.Java
import java.net.http.HttpClient
import org.koin.dsl.module
import ru.urent.qatools.core_network.HttpClientFactory

class CoreNetworkModule {
    val module = module {
        single<HttpClientFactory> {
            HttpClientFactoryImpl(
                engineFactory = Java,
                cachingEngineFactory = Java.config { protocolVersion = HttpClient.Version.HTTP_1_1 },
                log = get(),
            )
        }
    }
}
