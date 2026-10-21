package ru.urent.qatools.base_map

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.urent.qatools.base_bike_setup.entities.Parking
import ru.urent.qatools.base_bike_setup.entities.RestrictedZone
import ru.urent.qatools.core_entities.GeoBounds
import ru.urent.qatools.core_entities.GeoPoint

class MapZoneGeometryTest {

    @Test
    fun parkingsInsideAreaOrReachingItWithRadiusAreKept() {
        val inside = polygonParking(id = "inside", center = GeoPoint(43.425, 39.925))
        val circleReachingEdge = circleParking(id = "circle", center = GeoPoint(43.425, AREA_EAST + EDGE_GAP_DEGREES), radiusMeters = 100.0)
        val far = polygonParking(id = "far", center = GeoPoint(43.5, 40.1))

        val visible = parkingsWithin(listOf(inside, circleReachingEdge, far), Area)

        assertEquals(listOf("inside", "circle"), visible.map { it.id })
    }

    @Test
    fun restrictedZoneCoveringAreaIsKeptEvenWithCenterOutside() {
        val covering = RestrictedZone(
            id = "covering",
            center = GeoPoint(43.45, 39.95),
            outline = listOf(GeoPoint(43.40, 39.90), GeoPoint(43.50, 39.90), GeoPoint(43.50, 40.00), GeoPoint(43.40, 40.00)),
            isNoRiding = true,
        )
        val outside = RestrictedZone(
            id = "outside",
            center = GeoPoint(43.6, 40.2),
            outline = listOf(GeoPoint(43.59, 40.19), GeoPoint(43.61, 40.19), GeoPoint(43.61, 40.21)),
            isNoRiding = false,
        )

        val visible = restrictedZonesWithin(listOf(covering, outside), Area)

        assertEquals(listOf("covering"), visible.map { it.id })
    }

    private fun polygonParking(id: String, center: GeoPoint) = Parking(
        id = id,
        name = id,
        center = center,
        radiusMeters = null,
        outline = listOf(
            GeoPoint(center.latitude - POLYGON_HALF_SIZE_DEGREES, center.longitude - POLYGON_HALF_SIZE_DEGREES),
            GeoPoint(center.latitude + POLYGON_HALF_SIZE_DEGREES, center.longitude + POLYGON_HALF_SIZE_DEGREES),
        ),
        isActive = true,
    )

    private fun circleParking(id: String, center: GeoPoint, radiusMeters: Double) = Parking(
        id = id,
        name = id,
        center = center,
        radiusMeters = radiusMeters,
        outline = emptyList(),
        isActive = true,
    )

    private companion object {
        const val AREA_EAST = 39.93
        const val EDGE_GAP_DEGREES = 0.0005
        const val POLYGON_HALF_SIZE_DEGREES = 0.0002
        val Area = GeoBounds(south = 43.42, west = 39.92, north = 43.43, east = AREA_EAST)
    }
}
