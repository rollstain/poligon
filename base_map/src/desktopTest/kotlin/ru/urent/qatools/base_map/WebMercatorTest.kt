package ru.urent.qatools.base_map

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.urent.qatools.core_entities.GeoPoint

class WebMercatorTest {

    @Test
    fun zeroPointIsCenterOfWorld() {
        val point = WebMercator.toNormalized(GeoPoint(0.0, 0.0))
        assertEquals(0.5, point.x, 1e-12)
        assertEquals(0.5, point.y, 1e-12)
    }

    @Test
    fun adlerPointSurvivesRoundTrip() {
        val adler = GeoPoint(43.42485, 39.92816)
        val normalized = WebMercator.toNormalized(adler)
        val restored = WebMercator.toGeo(normalized.x, normalized.y)
        assertTrue(abs(restored.latitude - adler.latitude) < 1e-9)
        assertTrue(abs(restored.longitude - adler.longitude) < 1e-9)
    }

    @Test
    fun adlerPointFallsIntoStandardSlippyMapTile() {
        val normalized = WebMercator.toNormalized(GeoPoint(43.42485, 39.92816))
        val tilesPerSide = 1 shl 14
        assertEquals(10009, (normalized.x * tilesPerSide).toInt())
        assertEquals(5993, (normalized.y * tilesPerSide).toInt())
    }
}
