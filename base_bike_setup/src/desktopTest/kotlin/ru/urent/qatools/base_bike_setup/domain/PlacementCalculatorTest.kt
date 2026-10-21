package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.Parking
import ru.urent.qatools.base_bike_setup.entities.RentZone
import ru.urent.qatools.core_entities.GeoPoint

class PlacementCalculatorTest {
    private val square = listOf(
        GeoPoint(43.40, 39.90),
        GeoPoint(43.40, 39.95),
        GeoPoint(43.45, 39.95),
        GeoPoint(43.45, 39.90),
    )
    private val parkingCenter = GeoPoint(43.42485, 39.92816)
    private val zones = MapZones(
        parkings = listOf(
            Parking(
                id = "p1",
                name = "Аттракционы",
                center = parkingCenter,
                radiusMeters = 15.0,
                outline = emptyList(),
                isActive = true,
            ),
        ),
        restrictedZones = emptyList(),
        rentZones = listOf(RentZone(id = "rent", outline = square)),
    )

    @Test
    fun pointInsideSquareIsInside() {
        assertTrue(PlacementCalculator.isInsidePolygon(GeoPoint(43.42, 39.92), square))
    }

    @Test
    fun pointOutsideSquareIsOutside() {
        assertFalse(PlacementCalculator.isInsidePolygon(GeoPoint(43.50, 39.92), square))
    }

    @Test
    fun oneDegreeOfLatitudeIsAboutHundredElevenKilometers() {
        val distance = PlacementCalculator.distanceMeters(GeoPoint(43.0, 39.0), GeoPoint(44.0, 39.0))
        assertTrue(abs(distance - 111_195.0) < 100.0, "distance $distance")
    }

    @Test
    fun pointNextToCircleParkingIsOnParkingInsideRentZone() {
        val placement = PlacementCalculator.placementOf(GeoPoint(43.42490, 39.92816), zones)
        assertEquals("Аттракционы", placement.parkingName)
        assertTrue(placement.isInsideRentZone)
    }

    @Test
    fun pointFarFromParkingIsNotOnParking() {
        val placement = PlacementCalculator.placementOf(GeoPoint(43.43, 39.93), zones)
        assertNull(placement.parkingName)
    }
}
