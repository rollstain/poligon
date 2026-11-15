package ru.urent.qatools.base_map

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.EmptyMapZones
import ru.urent.qatools.core_entities.GeoBounds
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.testing_stand.SilentLog

@OptIn(ExperimentalTestApi::class)
class BikeMapReturnTest {

    @Test
    fun mapReopensWhereItWasLeft() = runComposeUiTest {
        val viewports = mutableListOf<GeoBounds>()
        var isMapShown by mutableStateOf(true)
        setContent {
            val screens = rememberSaveableStateHolder()
            if (isMapShown) {
                screens.SaveableStateProvider(MAP_SCREEN_KEY) {
                    TestMap(
                        pins = listOf(testPin(BikePlace)),
                        onViewportIdle = { viewports.add(it) },
                        onMapControllerReady = {},
                    )
                }
            }
        }
        onNodeWithText("К байку").performClick()
        waitUntilCenteredAt(this, viewports, BikePlace)

        isMapShown = false
        waitForIdle()
        isMapShown = true

        assertSettledCenter(this, viewports, BikePlace)
    }

    @Test
    fun focusShownBeforeLeavingIsNotShownAgainOnReturn() = runComposeUiTest {
        val viewports = mutableListOf<GeoBounds>()
        var isMapShown by mutableStateOf(true)
        var pins by mutableStateOf(listOf(testPin(BikePlace)))
        var mapController: BikeMapController? = null
        setContent {
            val screens = rememberSaveableStateHolder()
            if (isMapShown) {
                screens.SaveableStateProvider(MAP_SCREEN_KEY) {
                    TestMap(
                        pins = pins,
                        onViewportIdle = { viewports.add(it) },
                        onMapControllerReady = { mapController = it },
                    )
                }
            }
        }
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { mapController != null }
        runOnIdle { checkNotNull(mapController) { "BikeMap не отдал контроллер" }.focus(BikePlace) }
        waitUntilCenteredAt(this, viewports, BikePlace)
        pins = listOf(testPin(MovedBikePlace))
        onNodeWithText("К байку").performClick()
        waitUntilCenteredAt(this, viewports, MovedBikePlace)

        isMapShown = false
        waitForIdle()
        isMapShown = true

        assertSettledCenter(this, viewports, MovedBikePlace)
    }

    private fun waitUntilCenteredAt(test: ComposeUiTest, viewports: List<GeoBounds>, point: GeoPoint) {
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.isNotEmpty() && isCenteredAt(viewports.last(), point) }
    }

    private fun assertSettledCenter(test: ComposeUiTest, viewports: List<GeoBounds>, expected: GeoPoint) {
        val idleCountBeforeReturn = viewports.size
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.size > idleCountBeforeReturn }
        test.mainClock.advanceTimeBy(SETTLE_TIME_MS)
        test.waitForIdle()
        val settled = viewports.last()
        assertTrue(isCenteredAt(settled, expected), "expected: $expected, settled: $settled")
    }

    private fun isCenteredAt(viewport: GeoBounds, point: GeoPoint): Boolean {
        val latitudeOffset = (viewport.south + viewport.north) / EDGES_PER_AXIS - point.latitude
        val longitudeOffset = (viewport.west + viewport.east) / EDGES_PER_AXIS - point.longitude
        return latitudeOffset in -CENTER_TOLERANCE_DEGREES..CENTER_TOLERANCE_DEGREES &&
            longitudeOffset in -CENTER_TOLERANCE_DEGREES..CENTER_TOLERANCE_DEGREES
    }

    private fun testPin(location: GeoPoint) = BikePin(
        id = PIN_ID,
        label = PIN_ID,
        location = location,
        color = Color.Green,
        isSelected = true,
        badgeColor = null,
    )

    @Composable
    private fun TestMap(pins: List<BikePin>, onViewportIdle: (GeoBounds) -> Unit, onMapControllerReady: (BikeMapController) -> Unit) {
        BikeMap(
            tileSource = TileSource(urlTemplate = "https://tiles.test/{z}/{x}/{y}", subdomains = emptyList(), urlSuffix = ""),
            tileLoader = MapTileLoader(httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }), log = SilentLog),
            initialCenter = CityCenter,
            pins = pins,
            zones = EmptyMapZones,
            techZones = emptyList(),
            onMapClick = {},
            onPinMoved = { _, _ -> },
            onPinClick = {},
            onParkingAreaIdle = {},
            onMapControllerReady = onMapControllerReady,
            onViewportIdle = onViewportIdle,
            modifier = Modifier.size(MAP_SIZE_DP.dp),
        )
    }

    private companion object {
        const val MAP_SCREEN_KEY = "map-screen"
        const val PIN_ID = "4071"
        const val MAP_SIZE_DP = 400
        const val WAIT_TIMEOUT_MS = 10_000L
        const val SETTLE_TIME_MS = 2_000L
        const val CENTER_TOLERANCE_DEGREES = 0.0001
        const val EDGES_PER_AXIS = 2
        const val CITY_CENTER_LATITUDE = 43.42
        const val CITY_CENTER_LONGITUDE = 39.93
        const val PLACE_SHIFT_BEYOND_VIEWPORT_DEGREES = 0.01
        val CityCenter = GeoPoint(latitude = CITY_CENTER_LATITUDE, longitude = CITY_CENTER_LONGITUDE)
        val BikePlace = GeoPoint(
            latitude = CityCenter.latitude + PLACE_SHIFT_BEYOND_VIEWPORT_DEGREES,
            longitude = CityCenter.longitude + PLACE_SHIFT_BEYOND_VIEWPORT_DEGREES,
        )
        val MovedBikePlace = GeoPoint(
            latitude = BikePlace.latitude + PLACE_SHIFT_BEYOND_VIEWPORT_DEGREES,
            longitude = BikePlace.longitude + PLACE_SHIFT_BEYOND_VIEWPORT_DEGREES,
        )
    }
}
