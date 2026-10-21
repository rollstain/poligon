package ru.urent.qatools.base_bike_setup.entities

import ru.urent.qatools.core_entities.GeoPoint

data class MapZones(
    val parkings: List<Parking>,
    val restrictedZones: List<RestrictedZone>,
    val rentZones: List<RentZone>,
) {
    operator fun plus(other: MapZones) = MapZones(
        parkings = parkings + other.parkings,
        restrictedZones = restrictedZones + other.restrictedZones,
        rentZones = rentZones + other.rentZones,
    )
}

data class TechZone(val id: String, val name: String, val center: GeoPoint, val outline: List<GeoPoint>)

data class City(val id: String, val name: String, val center: GeoPoint)

data class CityTechZones(val city: City, val techZones: List<TechZone>)

data class Parking(
    val id: String,
    val name: String,
    val center: GeoPoint,
    val radiusMeters: Double?,
    val outline: List<GeoPoint>,
    val isActive: Boolean,
)

data class RestrictedZone(
    val id: String,
    val center: GeoPoint,
    val outline: List<GeoPoint>,
    val isNoRiding: Boolean,
)

data class RentZone(val id: String, val outline: List<GeoPoint>)

val EmptyMapZones = MapZones(parkings = emptyList(), restrictedZones = emptyList(), rentZones = emptyList())
