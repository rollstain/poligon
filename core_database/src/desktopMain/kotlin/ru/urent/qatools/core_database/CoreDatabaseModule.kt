package ru.urent.qatools.core_database

import org.koin.dsl.module
import ru.urent.qatools.core_platform.AppFiles

class CoreDatabaseModule {
    val module = module {
        single<DatabaseDriverProvider> { JdbcDatabaseDriverProvider(directory = get<AppFiles>().root) }
        single { CoreDatabase(get<DatabaseDriverProvider>().driver(CORE_DATABASE_FILE, CoreDatabase.Schema)) }
    }

    private companion object {
        const val CORE_DATABASE_FILE = "core.db"
    }
}
