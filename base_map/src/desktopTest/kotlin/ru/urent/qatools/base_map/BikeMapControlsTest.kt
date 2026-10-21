package ru.urent.qatools.base_map

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.EmptyMapZones
import ru.urent.qatools.core_entities.GeoBounds
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.testing_stand.SilentLog

@OptIn(ExperimentalTestApi::class)
class BikeMapControlsTest {

    @Test
    fun zoomInButtonZoomsTheMapInsideScrollablePage() = runComposeUiTest {
        val viewports = mutableListOf<GeoBounds>()
        setContent {
            Box(modifier = Modifier.size(PAGE_SIZE_DP.dp)) {
                Column(modifier = Modifier.verticalScroll(ScrollState(initial = 0))) {
                    TestMap(onViewportIdle = { viewports.add(it) }, modifier = Modifier.size(MAP_SIZE_DP.dp))
                    Spacer(modifier = Modifier.height(TAIL_HEIGHT_DP.dp))
                }
            }
        }
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.isNotEmpty() }
        val before = viewports.last()

        onNodeWithContentDescription("Приблизить").performClick()
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.last() != before }

        val after = viewports.last()
        assertTrue(after.east - after.west < before.east - before.west, "before: $before, after: $after")
        assertEquals((before.west + before.east) / 2, (after.west + after.east) / 2, CENTER_TOLERANCE_DEGREES)
        assertEquals((before.south + before.north) / 2, (after.south + after.north) / 2, CENTER_TOLERANCE_DEGREES)
    }

    @Test
    fun secondZoomClickDuringAnimationContinuesFromFirstTarget() = runComposeUiTest {
        val viewports = mutableListOf<GeoBounds>()
        setContent { TestMap(onViewportIdle = { viewports.add(it) }, modifier = Modifier.size(MAP_SIZE_DP.dp)) }
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.isNotEmpty() }
        val before = viewports.last()
        val idleCountBefore = viewports.size

        mainClock.autoAdvance = false
        onNodeWithContentDescription("Приблизить").performClick()
        mainClock.advanceTimeBy(SECOND_CLICK_DELAY_MS)
        onNodeWithContentDescription("Приблизить").performClick()
        mainClock.autoAdvance = true
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { viewports.size > idleCountBefore }

        val after = viewports.last()
        val zoomFactor = (before.east - before.west) / (after.east - after.west)
        assertEquals(DOUBLE_ZOOM_FACTOR, zoomFactor, ZOOM_FACTOR_TOLERANCE, "before: $before, after: $after")
        assertEquals((before.west + before.east) / 2, (after.west + after.east) / 2, CENTER_TOLERANCE_DEGREES)
        assertEquals((before.south + before.north) / 2, (after.south + after.north) / 2, CENTER_TOLERANCE_DEGREES)
    }

    @Test
    fun mouseWheelOverMapDoesNotScrollThePage() = runComposeUiTest {
        val pageScroll = ScrollState(initial = 0)
        setContent {
            Box(modifier = Modifier.size(PAGE_SIZE_DP.dp)) {
                Column(modifier = Modifier.verticalScroll(pageScroll)) {
                    TestMap(onViewportIdle = {}, modifier = Modifier.size(MAP_SIZE_DP.dp).testTag(MAP_TAG))
                    Spacer(modifier = Modifier.height(TAIL_HEIGHT_DP.dp))
                }
            }
        }

        onNodeWithTag(MAP_TAG).performMouseInput { scroll(WHEEL_DELTA) }
        waitForIdle()

        assertEquals(0, pageScroll.value)
    }

    @Test
    fun fitButtonSaysWhereItMovesTheMap() = runComposeUiTest {
        var pins by mutableStateOf(emptyList<BikePin>())
        setContent { TestMap(onViewportIdle = {}, modifier = Modifier.size(MAP_SIZE_DP.dp), pins = pins) }
        onNodeWithText("К байку").assertDoesNotExist()
        onNodeWithText("Все байки").assertDoesNotExist()

        pins = listOf(testPin("1"))
        onNodeWithText("К байку").assertExists()

        pins = listOf(testPin("1"), testPin("2"))
        onNodeWithText("Все байки").assertExists()
    }

    private fun testPin(id: String) = BikePin(
        id = id,
        label = id,
        location = GeoPoint(latitude = 43.42, longitude = 39.93),
        color = Color.Green,
        isSelected = false,
        badgeColor = null,
    )

    @Composable
    private fun TestMap(onViewportIdle: (GeoBounds) -> Unit, modifier: Modifier, pins: List<BikePin> = emptyList()) {
        BikeMap(
            tileSource = TileSource(urlTemplate = "https://tiles.test/{z}/{x}/{y}", subdomains = emptyList(), urlSuffix = ""),
            tileLoader = MapTileLoader(httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }), log = SilentLog),
            initialCenter = GeoPoint(latitude = 43.42, longitude = 39.93),
            focus = null,
            pins = pins,
            zones = EmptyMapZones,
            techZones = emptyList(),
            onMapClick = {},
            onPinMoved = { _, _ -> },
            onPinClick = {},
            onParkingAreaIdle = {},
            onViewportIdle = onViewportIdle,
            modifier = modifier,
        )
    }

    private companion object {
        const val MAP_TAG = "map"
        const val MAP_SIZE_DP = 400
        const val PAGE_SIZE_DP = 500
        const val TAIL_HEIGHT_DP = 1000
        const val WHEEL_DELTA = 3f
        const val WAIT_TIMEOUT_MS = 10_000L
        const val CENTER_TOLERANCE_DEGREES = 0.0001
        const val SECOND_CLICK_DELAY_MS = 100L
        const val DOUBLE_ZOOM_FACTOR = 4.0
        const val ZOOM_FACTOR_TOLERANCE = 0.01
    }
}
