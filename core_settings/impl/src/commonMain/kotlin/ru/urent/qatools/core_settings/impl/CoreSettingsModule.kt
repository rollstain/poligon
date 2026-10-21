package ru.urent.qatools.core_settings.impl

import org.koin.dsl.module
import ru.urent.qatools.core_settings.AppSettingsRepository

class CoreSettingsModule {
    val module = module {
        single<AppSettingsRepository> { AppSettingsRepositoryImpl(database = get()) }
    }
}
