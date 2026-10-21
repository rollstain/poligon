package ru.urent.qatools.data_templates.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ru.urent.qatools.data_templates.LastCityEntity
import ru.urent.qatools.data_templates.LastCityRepository
import ru.urent.qatools.data_templates.db.BikeSetupDatabase

internal class LastCityRepositoryImpl(database: BikeSetupDatabase) : LastCityRepository {
    private val queries = database.lastCityQueries

    override suspend fun getLastCity(standBaseUrl: String): LastCityEntity? = withContext(Dispatchers.IO) {
        val row = queries.selectLastCity(standBaseUrl).executeAsOneOrNull()
        if (row == null) null else LastCityEntity(id = row.city_id, name = row.city_name)
    }

    override suspend fun save(standBaseUrl: String, city: LastCityEntity): Unit = withContext(Dispatchers.IO) {
        queries.saveLastCity(stand_base_url = standBaseUrl, city_id = city.id, city_name = city.name)
    }
}
