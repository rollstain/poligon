package ru.urent.qatools.core_platform

import java.io.File
import org.koin.dsl.module

class CorePlatformModule {
    val module = module {
        single<TokenCipher> { DpapiTokenCipher() }
        single {
            val root = System.getenv(APP_DATA_ENV) ?: System.getProperty(USER_HOME_PROPERTY)
            AppFiles(root = File(root, APP_DATA_FOLDER))
        }
    }

    private companion object {
        const val APP_DATA_ENV = "APPDATA"
        const val USER_HOME_PROPERTY = "user.home"
        const val APP_DATA_FOLDER = "Poligon"
    }
}
