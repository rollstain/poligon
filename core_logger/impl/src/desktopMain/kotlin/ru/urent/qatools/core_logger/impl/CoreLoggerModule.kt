package ru.urent.qatools.core_logger.impl

import org.koin.dsl.module
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_platform.AppFiles

class CoreLoggerModule {
    val module = module {
        single<AppLog> { FileAppLog(directory = get<AppFiles>().logs) }
    }
}
