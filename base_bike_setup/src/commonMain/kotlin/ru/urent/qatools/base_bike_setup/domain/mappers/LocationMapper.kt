package ru.urent.qatools.base_bike_setup.domain.mappers

import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.CityTechZones
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.Parking
import ru.urent.qatools.base_bike_setup.entities.RentZone
import ru.urent.qatools.base_bike_setup.entities.RestrictedZone
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.data_location.ZoneType
import ru.urent.qatools.data_location.response.CitiesResponse
import ru.urent.qatools.data_location.response.CoordinateResponse
import ru.urent.qatools.data_location.response.ParkingListResponse
import ru.urent.qatools.data_location.response.TechZonesResponse

class LocationMapper {

    fun toCities(response: CitiesResponse): List<City> {
        val cities = mutableListOf<City>()
        for (entry in response.entries.orEmpty()) {
            val id = entry?.id
            val name = entry?.name
            val center = toGeoPoint(entry?.center)
            if (id == null || name == null || center == null || entry.isCity != true || entry.isDisabled == true) continue
            cities.add(City(id = id, name = name, center = center))
        }
        return cities.sortedBy { it.name.lowercase() }
    }

    fun toTechZones(response: TechZonesResponse): List<TechZone> {
        val zones = mutableListOf<TechZone>()
        for (entry in response.entries.orEmpty()) {
            val id = entry?.id
            val name = entry?.name
            val center = toGeoPoint(entry?.center)
            if (id == null || name == null || center == null || entry.isDisabled == true) continue
            zones.add(TechZone(id = id, name = name, center = center, outline = toGeoPoints(entry.coordinates.orEmpty())))
        }
        return zones.sortedBy { it.name.lowercase() }
    }

    fun toCityTechZones(response: TechZonesResponse, cities: List<City>): List<CityTechZones> {
        val techZonesByCityId = mutableMapOf<String, MutableList<TechZone>>()
        for (entry in response.entries.orEmpty()) {
            val id = entry?.id
            val name = entry?.name
            val cityId = entry?.areaId
            val center = toGeoPoint(entry?.center)
            if (id == null || name == null || cityId == null || center == null || entry.isDisabled == true) continue
            val techZone = TechZone(id = id, name = name, center = center, outline = toGeoPoints(entry.coordinates.orEmpty()))
            techZonesByCityId.getOrPut(cityId) { mutableListOf() }.add(techZone)
        }
        val cityTechZones = mutableListOf<CityTechZones>()
        for (city in cities) {
            val techZones = techZonesByCityId[city.id] ?: continue
            cityTechZones.add(CityTechZones(city = city, techZones = techZones.sortedBy { it.name.lowercase() }))
        }
        return cityTechZones
    }

    fun toMapZones(entries: List<ParkingListResponse.Entry?>): MapZones {
        val parkings = mutableListOf<Parking>()
        val restrictedZones = mutableListOf<RestrictedZone>()
        val rentZones = mutableListOf<RentZone>()
        for (entry in entries) {
            val id = entry?.id
            val type = entry?.type
            if (id == null || type == null) continue
            val outline = toGeoPoints(entry.coordinates.orEmpty())
            val center = toGeoPoint(entry.center)
            when (type) {
                ZoneType.PARKING, ZoneType.CIRCLE_PARKING -> {
                    if (center == null) continue
                    val radius = if (type == ZoneType.CIRCLE_PARKING) entry.radius else null
                    parkings.add(
                        Parking(
                            id = id,
                            name = entry.name.orEmpty(),
                            center = center,
                            radiusMeters = radius,
                            outline = outline,
                            isActive = entry.isDisabled != true,
                        ),
                    )
                }
                ZoneType.NOT_ALLOWED, ZoneType.RESTRICTED -> {
                    if (center == null || outline.isEmpty()) continue
                    restrictedZones.add(
                        RestrictedZone(id = id, center = center, outline = outline, isNoRiding = type == ZoneType.NOT_ALLOWED),
                    )
                }
                ZoneType.RENT -> {
                    if (outline.isEmpty()) continue
                    rentZones.add(RentZone(id = id, outline = outline))
                }
            }
        }
        return MapZones(parkings = parkings, restrictedZones = restrictedZones, rentZones = rentZones)
    }

    private fun toGeoPoints(coordinates: List<CoordinateResponse?>): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        for (coordinate in coordinates) {
            val point = toGeoPoint(coordinate) ?: continue
            points.add(point)
        }
        return points
    }

    private fun toGeoPoint(coordinate: CoordinateResponse?): GeoPoint? {
        val latitude = coordinate?.lat
        val longitude = coordinate?.lng
        if (latitude == null || longitude == null) return null
        return GeoPoint(latitude = latitude, longitude = longitude)
    }
}
