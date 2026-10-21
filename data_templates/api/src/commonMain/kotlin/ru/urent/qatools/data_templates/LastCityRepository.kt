package ru.urent.qatools.data_templates

interface LastCityRepository {
    suspend fun getLastCity(standBaseUrl: String): LastCityEntity?
    suspend fun save(standBaseUrl: String, city: LastCityEntity)
}
