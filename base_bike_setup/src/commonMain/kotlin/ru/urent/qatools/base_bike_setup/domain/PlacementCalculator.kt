package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.Placement
import ru.urent.qatools.core_entities.GeoPoint

object PlacementCalculator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val DEGREES_IN_HALF_TURN = 180.0

    fun placementOf(point: GeoPoint, zones: MapZones): Placement {
        var parkingName: String? = null
        for (parking in zones.parkings) {
            val radius = parking.radiusMeters
            val isInside = if (radius != null && radius > 0) {
                distanceMeters(point, parking.center) <= radius
            } else {
                isInsidePolygon(point, parking.outline)
            }
            if (isInside) {
                parkingName = parking.name
                break
            }
        }
        var isInsideNoRidingZone = false
        var isInsideNoParkingZone = false
        for (zone in zones.restrictedZones) {
            if (!isInsidePolygon(point, zone.outline)) continue
            if (zone.isNoRiding) isInsideNoRidingZone = true else isInsideNoParkingZone = true
        }
        return Placement(
            parkingName = parkingName,
            isInsideRentZone = zones.rentZones.any { isInsidePolygon(point, it.outline) },
            isInsideNoRidingZone = isInsideNoRidingZone,
            isInsideNoParkingZone = isInsideNoParkingZone,
        )
    }

    fun isInsidePolygon(point: GeoPoint, outline: List<GeoPoint>): Boolean {
        var isInside = false
        var previous = outline.lastIndex
        for (current in outline.indices) {
            val a = outline[current]
            val b = outline[previous]
            val crossesLatitude = (a.latitude > point.latitude) != (b.latitude > point.latitude)
            if (crossesLatitude) {
                val longitudeAtCrossing = a.longitude +
                    (point.latitude - a.latitude) / (b.latitude - a.latitude) * (b.longitude - a.longitude)
                if (point.longitude < longitudeAtCrossing) isInside = !isInside
            }
            previous = current
        }
        return isInside
    }

    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val fromLatitude = toRadians(from.latitude)
        val toLatitude = toRadians(to.latitude)
        val deltaLatitude = toRadians(to.latitude - from.latitude)
        val deltaLongitude = toRadians(to.longitude - from.longitude)
        val halfChord = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
            cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(halfChord))
    }

    private fun toRadians(degrees: Double): Double = degrees * PI / DEGREES_IN_HALF_TURN
}
