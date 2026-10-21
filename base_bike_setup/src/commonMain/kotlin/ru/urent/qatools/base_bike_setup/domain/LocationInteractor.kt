package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.PI
import kotlin.math.cos
import ru.urent.qatools.base_bike_setup.domain.mappers.LocationMapper
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.CityTechZones
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.SavedCity
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_location.LocationRepository
import ru.urent.qatools.data_templates.LastCityEntity
import ru.urent.qatools.data_templates.LastCityRepository

class LocationInteractor(
    private val locationRepository: LocationRepository,
    private val lastCityRepository: LastCityRepository,
    private val locationMapper: LocationMapper,
) {
    private val cityZonesCache = mutableMapOf<Pair<String, String>, MapZones>()
    private val cityTechZonesCache = mutableMapOf<String, List<CityTechZones>>()

    suspend fun getCities(): List<City> {
        val loaded = locationRepository.getCities() as? RequestResult.Success ?: return emptyList()
        return locationMapper.toCities(loaded.data)
    }

    suspend fun getTechZones(cityId: String): List<TechZone> {
        val loaded = locationRepository.getTechZones(listOf(cityId)) as? RequestResult.Success ?: return emptyList()
        return locationMapper.toTechZones(loaded.data)
    }

    suspend fun getTechZonesOfCities(standBaseUrl: String, cities: List<City>): List<CityTechZones> {
        val cached = cityTechZonesCache[standBaseUrl]
        if (cached != null) return cached
        val loaded = locationRepository.getTechZones(cities.map { it.id }) as? RequestResult.Success ?: return emptyList()
        val cityTechZones = locationMapper.toCityTechZones(response = loaded.data, cities = cities)
        cityTechZonesCache[standBaseUrl] = cityTechZones
        return cityTechZones
    }

    suspend fun getCityZones(standBaseUrl: String, cityId: String): RequestResult<MapZones> {
        val cacheKey = standBaseUrl to cityId
        val cached = cityZonesCache[cacheKey]
        if (cached != null) return RequestResult.Success(cached)
        return when (val loaded = locationRepository.getCityZones(cityId)) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val zones = locationMapper.toMapZones(loaded.data)
                cityZonesCache[cacheKey] = zones
                RequestResult.Success(zones)
            }
        }
    }

    fun getNearestCity(cities: List<City>, point: GeoPoint): City? {
        val longitudeScale = cos(point.latitude * PI / DEGREES_IN_HALF_TURN)
        var nearest: City? = null
        var nearestSquaredDistance = Double.MAX_VALUE
        for (city in cities) {
            val latitudeDelta = city.center.latitude - point.latitude
            val longitudeDelta = (city.center.longitude - point.longitude) * longitudeScale
            val squaredDistance = latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
            if (squaredDistance >= nearestSquaredDistance) continue
            nearest = city
            nearestSquaredDistance = squaredDistance
        }
        return nearest
    }

    suspend fun getLastCity(standBaseUrl: String): SavedCity? {
        val city = lastCityRepository.getLastCity(standBaseUrl) ?: return null
        return SavedCity(id = city.id, name = city.name)
    }

    suspend fun saveLastCity(standBaseUrl: String, city: SavedCity) =
        lastCityRepository.save(standBaseUrl = standBaseUrl, city = LastCityEntity(id = city.id, name = city.name))

    private companion object {
        const val DEGREES_IN_HALF_TURN = 180.0
    }
}
