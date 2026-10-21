package ru.urent.qatools.base_map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import ru.urent.qatools.base_bike_setup.entities.Parking
import ru.urent.qatools.base_bike_setup.entities.RestrictedZone
import ru.urent.qatools.core_entities.GeoBounds
import ru.urent.qatools.core_entities.GeoPoint

private const val CIRCLE_SEGMENTS = 48
private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
private const val DEGREES_IN_HALF_TURN = 180.0

internal fun parkingsWithin(parkings: List<Parking>, area: GeoBounds): List<Parking> {
    val visible = mutableListOf<Parking>()
    for (parking in parkings) {
        val radius = parking.radiusMeters
        val bounds = if (radius != null && radius > 0) circleBounds(parking.center, radius) else outlineBounds(parking.outline, parking.center)
        if (intersects(bounds, area)) visible.add(parking)
    }
    return visible
}

internal fun restrictedZonesWithin(zones: List<RestrictedZone>, area: GeoBounds): List<RestrictedZone> {
    val visible = mutableListOf<RestrictedZone>()
    for (zone in zones) {
        if (intersects(outlineBounds(zone.outline, zone.center), area)) visible.add(zone)
    }
    return visible
}

internal fun circleOutline(center: GeoPoint, radiusMeters: Double): List<GeoPoint> {
    val longitudeMetersPerDegree = METERS_PER_LATITUDE_DEGREE * cos(center.latitude * PI / DEGREES_IN_HALF_TURN)
    val points = mutableListOf<GeoPoint>()
    for (step in 0 until CIRCLE_SEGMENTS) {
        val angle = 2 * PI * step / CIRCLE_SEGMENTS
        val latitudeOffset = radiusMeters / METERS_PER_LATITUDE_DEGREE * sin(angle)
        val longitudeOffset = radiusMeters / longitudeMetersPerDegree * cos(angle)
        points.add(GeoPoint(latitude = center.latitude + latitudeOffset, longitude = center.longitude + longitudeOffset))
    }
    return points
}

private fun circleBounds(center: GeoPoint, radiusMeters: Double): GeoBounds {
    val latitudeOffset = radiusMeters / METERS_PER_LATITUDE_DEGREE
    val longitudeOffset = radiusMeters / (METERS_PER_LATITUDE_DEGREE * cos(center.latitude * PI / DEGREES_IN_HALF_TURN))
    return GeoBounds(
        south = center.latitude - latitudeOffset,
        west = center.longitude - longitudeOffset,
        north = center.latitude + latitudeOffset,
        east = center.longitude + longitudeOffset,
    )
}

private fun outlineBounds(outline: List<GeoPoint>, center: GeoPoint): GeoBounds {
    var south = center.latitude
    var west = center.longitude
    var north = center.latitude
    var east = center.longitude
    for (point in outline) {
        south = min(south, point.latitude)
        west = min(west, point.longitude)
        north = max(north, point.latitude)
        east = max(east, point.longitude)
    }
    return GeoBounds(south = south, west = west, north = north, east = east)
}

private fun intersects(bounds: GeoBounds, area: GeoBounds): Boolean =
    bounds.south <= area.north && bounds.north >= area.south && bounds.west <= area.east && bounds.east >= area.west
