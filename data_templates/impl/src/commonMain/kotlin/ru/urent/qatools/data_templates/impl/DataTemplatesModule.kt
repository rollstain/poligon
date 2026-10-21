package ru.urent.qatools.data_templates.impl

import org.koin.dsl.module
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.data_templates.LastCityRepository
import ru.urent.qatools.data_templates.TemplateRepository
import ru.urent.qatools.data_templates.db.BikeSetupDatabase

class DataTemplatesModule {
    val module = module {
        single { BikeSetupDatabase(get<DatabaseDriverProvider>().driver(BIKE_SETUP_DATABASE_FILE, BikeSetupDatabase.Schema)) }
        single<TemplateRepository> { TemplateRepositoryImpl(database = get()) }
        single<LastCityRepository> { LastCityRepositoryImpl(database = get()) }
    }

    private companion object {
        const val BIKE_SETUP_DATABASE_FILE = "bike-setup.db"
    }
}
