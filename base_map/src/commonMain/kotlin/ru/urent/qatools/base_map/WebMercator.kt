package ru.urent.qatools.base_map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan
import ru.urent.qatools.core_entities.GeoPoint

internal data class NormalizedPoint(val x: Double, val y: Double)

internal object WebMercator {
    private const val FULL_TURN_DEGREES = 360.0
    private const val HALF_TURN_DEGREES = 180.0

    fun toNormalized(point: GeoPoint): NormalizedPoint {
        val latitudeRadians = point.latitude * PI / HALF_TURN_DEGREES
        val x = (point.longitude + HALF_TURN_DEGREES) / FULL_TURN_DEGREES
        val y = (1 - ln(tan(latitudeRadians) + 1 / cos(latitudeRadians)) / PI) / 2
        return NormalizedPoint(x = x, y = y)
    }

    fun toGeo(x: Double, y: Double): GeoPoint {
        val longitude = x * FULL_TURN_DEGREES - HALF_TURN_DEGREES
        val latitude = atan(sinh(PI * (1 - 2 * y))) * HALF_TURN_DEGREES / PI
        return GeoPoint(latitude = latitude, longitude = longitude)
    }
}
