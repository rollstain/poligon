package ru.urent.qatools.base_map

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
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
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.testing_stand.SilentLog

@OptIn(ExperimentalTestApi::class)
class BikePinDragTest {

    @Test
    fun slowDragOfPinBodyMovesPin() = runComposeUiTest { assertDragMovesPin(this, isSelected = false, grabAboveAnchorDp = WIDE_BODY_ABOVE_ANCHOR_DP, steps = SLOW_STEPS) }

    @Test
    fun quickDragOfPinBodyMovesPin() = runComposeUiTest { assertDragMovesPin(this, isSelected = false, grabAboveAnchorDp = WIDE_BODY_ABOVE_ANCHOR_DP, steps = QUICK_STEPS) }

    @Test
    fun dragOfSelectedPinBodyMovesPin() = runComposeUiTest { assertDragMovesPin(this, isSelected = true, grabAboveAnchorDp = ACTIVE_BODY_ABOVE_ANCHOR_DP, steps = SLOW_STEPS) }

    @Test
    fun dragByPinTailMovesPin() = runComposeUiTest { assertDragMovesPin(this, isSelected = false, grabAboveAnchorDp = TAIL_ABOVE_ANCHOR_DP, steps = SLOW_STEPS) }

    private fun assertDragMovesPin(test: androidx.compose.ui.test.ComposeUiTest, isSelected: Boolean, grabAboveAnchorDp: Int, steps: Int) {
        val moves = mutableListOf<GeoPoint>()
        test.setContent {
            BikeMap(
                tileSource = TileSource(urlTemplate = "https://tiles.test/{z}/{x}/{y}", subdomains = emptyList(), urlSuffix = ""),
                tileLoader = MapTileLoader(httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }), log = SilentLog),
                initialCenter = PinLocation,
                focus = null,
                pins = listOf(BikePin(id = PIN_ID, label = PIN_ID, location = PinLocation, color = Color.Green, isSelected = isSelected, badgeColor = null)),
                zones = EmptyMapZones,
                techZones = emptyList(),
                onMapClick = {},
                onPinMoved = { _, location -> moves.add(location) },
                onPinClick = {},
                onParkingAreaIdle = {},
                modifier = Modifier.size(MAP_SIZE_DP.dp).testTag(MAP_TAG),
            )
        }
        test.waitForIdle()

        test.onNodeWithTag(MAP_TAG).performMouseInput {
            val grab = Offset(x = center.x, y = center.y - grabAboveAnchorDp.dp.toPx())
            moveTo(grab)
            press()
            for (step in 1..steps) moveTo(grab + Offset(x = DRAG_DISTANCE_DP.dp.toPx() * step / steps, y = 0f))
            release()
        }
        test.waitForIdle()

        assertEquals(1, moves.size, "Пин должен переехать один раз: $moves")
        assertTrue(moves.single().longitude > PinLocation.longitude, "Пин должен уехать на восток: ${moves.single()}")
    }

    private companion object {
        const val PIN_ID = "4071"
        const val MAP_TAG = "map"
        const val MAP_SIZE_DP = 400
        const val WIDE_BODY_ABOVE_ANCHOR_DP = 29
        const val ACTIVE_BODY_ABOVE_ANCHOR_DP = 42
        const val TAIL_ABOVE_ANCHOR_DP = 6
        const val SLOW_STEPS = 10
        const val QUICK_STEPS = 1
        const val DRAG_DISTANCE_DP = 80
        val PinLocation = GeoPoint(latitude = 43.42, longitude = 39.93)
    }
}
