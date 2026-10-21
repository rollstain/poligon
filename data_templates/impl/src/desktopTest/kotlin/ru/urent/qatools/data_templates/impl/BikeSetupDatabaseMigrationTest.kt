package ru.urent.qatools.data_templates.impl

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.data_templates.LastCityEntity
import ru.urent.qatools.data_templates.StoredAvailability
import ru.urent.qatools.data_templates.db.BikeSetupDatabase

class BikeSetupDatabaseMigrationTest {

    @Test
    fun bikesOfFirstVersionKeepAvailabilityAfterMigration() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, FIRST_VERSION_TEMPLATE_TABLE, 0)
        driver.execute(null, FIRST_VERSION_BIKE_TABLE, 0)
        driver.execute(null, "INSERT INTO template(stand_base_url, name) VALUES ('$STAND', 'Смоук QA')", 0)
        driver.execute(
            null,
            "INSERT INTO template_bike(template_id, simulator_id, latitude, longitude, target_state, target_percent) " +
                "VALUES (1, $SIMULATOR_ID, 43.42, 39.93, 'LOW', NULL)",
            0,
        )

        BikeSetupDatabase.Schema.migrate(driver, FIRST_SCHEMA_VERSION, BikeSetupDatabase.Schema.version)
        val database = BikeSetupDatabase(driver)
        val settings = TemplateRepositoryImpl(database).getTemplate(1).first()?.bikes.orEmpty().single().setup.settings
        val cities = LastCityRepositoryImpl(database)
        cities.save(standBaseUrl = STAND, city = AdlerCity)

        assertEquals(StoredAvailability.KEEP, settings.availability)
        assertEquals(ChargeState.LOW, settings.targetState)
        assertNull(settings.targetPercent)
        assertEquals(AdlerCity, cities.getLastCity(STAND))
    }

    private companion object {
        const val STAND = "https://tm-0.dev.urent.ru"
        const val SIMULATOR_ID = 4071
        const val FIRST_SCHEMA_VERSION = 1L
        val AdlerCity = LastCityEntity(id = "adler", name = "Адлер")
        const val FIRST_VERSION_TEMPLATE_TABLE = """
            CREATE TABLE template (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stand_base_url TEXT NOT NULL,
                name TEXT NOT NULL
            )
        """
        const val FIRST_VERSION_BIKE_TABLE = """
            CREATE TABLE template_bike (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                template_id INTEGER NOT NULL REFERENCES template(id) ON DELETE CASCADE,
                simulator_id INTEGER NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                target_state TEXT,
                target_percent REAL,
                CHECK ((target_state IS NULL) <> (target_percent IS NULL))
            )
        """
    }
}
