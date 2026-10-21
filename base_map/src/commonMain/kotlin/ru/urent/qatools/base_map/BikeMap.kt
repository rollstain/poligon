package ru.urent.qatools.base_map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.idleStateFlow
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.minScale
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.api.updateMarkerOffset
import ovh.plrapps.mapcompose.api.updateMarkerZ
import ovh.plrapps.mapcompose.api.visibleBoundingBox
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
import ovh.plrapps.mapcompose.ui.state.MapState
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.Parking
import ru.urent.qatools.base_bike_setup.entities.RestrictedZone
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.core_entities.GeoBounds
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

data class BikePin(
    val id: String,
    val label: String,
    val location: GeoPoint,
    val color: Color,
    val isSelected: Boolean,
    val badgeColor: Color?,
)

class MapFocus(val point: GeoPoint)

private const val MAX_ZOOM_LEVEL = 18
private const val TILE_SIZE_PX = 512
private const val FULL_SIZE_PX = TILE_SIZE_PX shl MAX_ZOOM_LEVEL
private const val INITIAL_ZOOM_LEVEL = 16
private const val FOCUS_ZOOM_LEVEL = 17
private const val ZOOM_STEP = 2.0
private const val FIT_PADDING_RATIO = 0.3f
private const val PARKINGS_MIN_ZOOM_LEVEL = 15
private const val RESTRICTED_ZONES_MIN_ZOOM_LEVEL = 14
private const val PARKING_FILL_ALPHA = 0.15f
private const val RESTRICTED_FILL_ALPHA = 0.1f
private const val SELECTED_BIKE_Z_INDEX = 11f
private const val BIKE_Z_INDEX = 10f
private const val PARKING_Z_INDEX = 4f
private const val NO_RIDING_SIGN_Z_INDEX = 2f
private const val NO_PARKING_SIGN_Z_INDEX = 1f
private const val RENT_PATH_PREFIX = "rent-"
private const val RESTRICTED_PATH_PREFIX = "restricted-"
private const val PARKING_PATH_PREFIX = "parking-"
private const val SIGN_MARKER_PREFIX = "sign-"
private const val PARKING_MARKER_PREFIX = "parking-marker-"
private const val TECH_ZONE_PATH_PREFIX = "tech-zone-"
private const val TECH_ZONE_LABEL_PREFIX = "tech-zone-label-"
private const val TECH_ZONE_LABEL_Z_INDEX = 3f
private const val CONTROL_STROKE_DP = 1.6f
private const val FIT_ICON_CORNER_RATIO = 0.3f
private const val BUTTON_ZOOM_DURATION_MS = 300

private val PinRelativeOffset = Offset(-0.5f, 0f)
private val CenteredRelativeOffset = Offset(-0.5f, -0.5f)
private val ParkingStrokeWidth = 2.dp
private val RestrictedStrokeWidth = 1.dp
private val RentStrokeWidth = 2.dp
private val ParkingDash = 4.dp
private val TechZoneStrokeWidth = 2.dp
private val TechZoneDash = 7.dp
private val TechZoneGap = 5.dp
private val ControlSize = 44.dp
private val FitIconSize = 16.dp
private val ButtonZoomSpec = tween<Float>(durationMillis = BUTTON_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)

@Composable
fun BikeMap(
    tileSource: TileSource,
    tileLoader: MapTileLoader,
    initialCenter: GeoPoint,
    focus: MapFocus?,
    pins: List<BikePin>,
    zones: MapZones,
    techZones: List<TechZone>,
    onMapClick: (GeoPoint) -> Unit,
    onPinMoved: (pinId: String, location: GeoPoint) -> Unit,
    onPinClick: (pinId: String) -> Unit,
    onParkingAreaIdle: (center: GeoPoint) -> Unit,
    onViewportIdle: (GeoBounds) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnPinMoved by rememberUpdatedState(onPinMoved)
    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnViewportIdle by rememberUpdatedState(onViewportIdle)
    val currentOnParkingAreaIdle by rememberUpdatedState(onParkingAreaIdle)
    val currentPins by rememberUpdatedState(pins)
    val shownPins = remember { mutableStateMapOf<String, BikePin>() }
    val shownRentPathIds = remember { mutableListOf<String>() }
    val shownParkings = remember { mutableMapOf<String, Parking>() }
    val shownRestrictedZones = remember { mutableMapOf<String, RestrictedZone>() }
    var parkingArea by remember { mutableStateOf<GeoBounds?>(null) }
    var restrictedZoneArea by remember { mutableStateOf<GeoBounds?>(null) }
    val shownTechZoneIds = remember { mutableListOf<String>() }
    val scope = rememberCoroutineScope()
    var buttonZoomJob by remember { mutableStateOf<Job?>(null) }
    var buttonZoomTargetScale by remember { mutableStateOf(0.0) }

    val mapState = remember(tileSource) {
        val start = WebMercator.toNormalized(initialCenter)
        val state = MapState(
            levelCount = MAX_ZOOM_LEVEL + 1,
            fullWidth = FULL_SIZE_PX,
            fullHeight = FULL_SIZE_PX,
            tileSize = TILE_SIZE_PX,
        ) {
            scroll(start.x, start.y)
            scale(scaleForZoom(INITIAL_ZOOM_LEVEL))
        }
        state.addLayer(TileStreamProvider { row, col, zoomLevel -> tileLoader.loadTile(tileSource, row, col, zoomLevel) })
        state.setMapBackground(MapColors.mapBackground)
        state.onTap { x, y -> currentOnMapClick(WebMercator.toGeo(x, y)) }
        state.onMarkerClick { id, _, _ -> if (shownPins.containsKey(id)) currentOnPinClick(id) }
        state
    }
    DisposableEffect(mapState) { onDispose { mapState.shutdown() } }

    LaunchedEffect(mapState, pins) {
        val pinIds = pins.map { it.id }.toSet()
        for (id in shownPins.keys.toList()) {
            if (id in pinIds) continue
            mapState.removeMarker(id)
            shownPins.remove(id)
        }
        for (pin in pins) {
            val point = WebMercator.toNormalized(pin.location)
            val previous = shownPins[pin.id]
            shownPins[pin.id] = pin
            if (previous == null) {
                mapState.addMarker(
                    id = pin.id,
                    x = point.x,
                    y = point.y,
                    relativeOffset = PinRelativeOffset,
                    absoluteOffset = pinAnchorOffset(pin.isSelected),
                    zIndex = if (pin.isSelected) SELECTED_BIKE_Z_INDEX else BIKE_Z_INDEX,
                ) {
                    val shown = shownPins[pin.id]
                    if (shown != null) {
                        BikePinMarker(
                            label = shown.label,
                            color = shown.color,
                            isSelected = shown.isSelected,
                            badgeColor = shown.badgeColor,
                        )
                    }
                }
                mapState.enableMarkerDrag(
                    id = pin.id,
                    onDragEnd = { id, x, y -> currentOnPinMoved(id, WebMercator.toGeo(x, y)) },
                )
                continue
            }
            if (previous.location != pin.location) mapState.moveMarker(pin.id, point.x, point.y)
            if (previous.isSelected != pin.isSelected) {
                mapState.updateMarkerZ(pin.id, if (pin.isSelected) SELECTED_BIKE_Z_INDEX else BIKE_Z_INDEX)
                mapState.updateMarkerOffset(id = pin.id, absoluteOffset = pinAnchorOffset(pin.isSelected), animationSpec = null)
            }
        }
    }

    LaunchedEffect(mapState, zones.rentZones) {
        for (id in shownRentPathIds) mapState.removePath(id)
        shownRentPathIds.clear()
        for (rentZone in zones.rentZones) {
            val pathData = closedPathData(mapState, rentZone.outline) ?: continue
            val id = RENT_PATH_PREFIX + rentZone.id
            mapState.addPath(id = id, pathData = pathData, width = RentStrokeWidth, color = MapColors.rentZone)
            shownRentPathIds.add(id)
        }
    }

    LaunchedEffect(mapState, zones.parkings, parkingArea) {
        val area = parkingArea
        val visibleParkings = if (area == null) emptyList() else parkingsWithin(zones.parkings, area)
        val wantedParkings = visibleParkings.associateBy { it.id }
        for ((id, shown) in shownParkings.toList()) {
            if (wantedParkings[id] == shown) continue
            removeParking(mapState, id)
            shownParkings.remove(id)
        }
        for (parking in visibleParkings) {
            if (parking.id in shownParkings) continue
            addParking(mapState, parking)
            shownParkings[parking.id] = parking
        }
    }

    LaunchedEffect(mapState, zones.restrictedZones, restrictedZoneArea) {
        val area = restrictedZoneArea
        val visibleRestrictedZones = if (area == null) emptyList() else restrictedZonesWithin(zones.restrictedZones, area)
        val wantedRestrictedZones = visibleRestrictedZones.associateBy { it.id }
        for ((id, shown) in shownRestrictedZones.toList()) {
            if (wantedRestrictedZones[id] == shown) continue
            removeRestrictedZone(mapState, id)
            shownRestrictedZones.remove(id)
        }
        for (zone in visibleRestrictedZones) {
            if (zone.id in shownRestrictedZones) continue
            addRestrictedZone(mapState, zone)
            shownRestrictedZones[zone.id] = zone
        }
    }

    LaunchedEffect(mapState, techZones) {
        for (id in shownTechZoneIds) {
            mapState.removePath(TECH_ZONE_PATH_PREFIX + id)
            mapState.removeMarker(TECH_ZONE_LABEL_PREFIX + id)
        }
        shownTechZoneIds.clear()
        for (techZone in techZones) {
            val pathData = closedPathData(mapState, techZone.outline) ?: continue
            mapState.addPath(
                id = TECH_ZONE_PATH_PREFIX + techZone.id,
                pathData = pathData,
                width = TechZoneStrokeWidth,
                color = MapColors.techZone,
                pattern = listOf(PatternItem.Dash(TechZoneDash), PatternItem.Gap(TechZoneGap)),
            )
            val center = WebMercator.toNormalized(techZone.center)
            mapState.addMarker(
                id = TECH_ZONE_LABEL_PREFIX + techZone.id,
                x = center.x,
                y = center.y,
                relativeOffset = CenteredRelativeOffset,
                zIndex = TECH_ZONE_LABEL_Z_INDEX,
                clickable = false,
            ) { TechZoneLabel(name = techZone.name) }
            shownTechZoneIds.add(techZone.id)
        }
    }

    LaunchedEffect(mapState) {
        mapState.idleStateFlow().collect { isIdle ->
            if (!isIdle) return@collect
            val box = mapState.visibleBoundingBox()
            val northWest = WebMercator.toGeo(box.xLeft, box.yTop)
            val southEast = WebMercator.toGeo(box.xRight, box.yBottom)
            val viewport = GeoBounds(
                south = southEast.latitude,
                west = northWest.longitude,
                north = northWest.latitude,
                east = southEast.longitude,
            )
            currentOnViewportIdle(viewport)
            val isParkingZoom = mapState.scale >= scaleForZoom(PARKINGS_MIN_ZOOM_LEVEL)
            parkingArea = if (isParkingZoom) viewport else null
            restrictedZoneArea = if (mapState.scale < scaleForZoom(RESTRICTED_ZONES_MIN_ZOOM_LEVEL)) null else viewport
            if (isParkingZoom) currentOnParkingAreaIdle(WebMercator.toGeo(mapState.centroidX, mapState.centroidY))
        }
    }

    LaunchedEffect(mapState, focus) {
        if (focus == null) return@LaunchedEffect
        val point = WebMercator.toNormalized(focus.point)
        mapState.scrollTo(point.x, point.y, destScale = max(mapState.scale, scaleForZoom(FOCUS_ZOOM_LEVEL)))
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type == PointerEventType.Scroll) {
                        for (change in event.changes) change.consume()
                    }
                }
            }
        },
    ) {
        MapUI(modifier = Modifier.fillMaxSize(), state = mapState)
        fun zoomAroundCenter(factor: Double) {
            val isPreviousZoomRunning = buttonZoomJob?.isActive == true
            val fromScale = if (isPreviousZoomRunning) buttonZoomTargetScale else mapState.scale
            val targetScale = (fromScale * factor).coerceIn(mapState.minScale, mapState.maxScale)
            buttonZoomTargetScale = targetScale
            buttonZoomJob = scope.launch {
                mapState.scrollTo(mapState.centroidX, mapState.centroidY, destScale = targetScale, animationSpec = ButtonZoomSpec)
            }
        }
        MapControls(
            onZoomIn = { zoomAroundCenter(ZOOM_STEP) },
            onZoomOut = { zoomAroundCenter(1 / ZOOM_STEP) },
            fitPinsLabel = when (pins.size) {
                0 -> null
                1 -> "К байку"
                else -> "Все байки"
            },
            onFitPins = { scope.launch { fitPins(mapState, currentPins) } },
            modifier = Modifier.align(Alignment.TopEnd).padding(QaDimens.spaceM),
        )
    }
}

@Composable
private fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    fitPinsLabel: String?,
    onFitPins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            onClick = onZoomIn,
            shape = CircleShape,
            color = QaColors.surface,
            contentColor = QaColors.textPrimary,
            shadowElevation = QaDimens.spaceXs,
            modifier = Modifier.size(ControlSize).semantics { contentDescription = "Приблизить" },
        ) {
            Box(contentAlignment = Alignment.Center) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        Surface(
            onClick = onZoomOut,
            shape = CircleShape,
            color = QaColors.surface,
            contentColor = QaColors.textPrimary,
            shadowElevation = QaDimens.spaceXs,
            modifier = Modifier.size(ControlSize).semantics { contentDescription = "Отдалить" },
        ) {
            Box(contentAlignment = Alignment.Center) { Text("−", style = MaterialTheme.typography.titleLarge) }
        }
        if (fitPinsLabel != null) {
            Surface(
                onClick = onFitPins,
                shape = CircleShape,
                color = QaColors.surface,
                contentColor = QaColors.textPrimary,
                shadowElevation = QaDimens.spaceXs,
                modifier = Modifier.height(ControlSize),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = QaDimens.spaceL),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    val fitIconColor = QaColors.textPrimary
                    Canvas(modifier = Modifier.size(FitIconSize)) {
                        val corner = size.minDimension * FIT_ICON_CORNER_RATIO
                        val strokeWidth = CONTROL_STROKE_DP * density
                        val corners = listOf(
                            Triple(Offset(0f, corner), Offset.Zero, Offset(corner, 0f)),
                            Triple(Offset(size.width - corner, 0f), Offset(size.width, 0f), Offset(size.width, corner)),
                            Triple(Offset(0f, size.height - corner), Offset(0f, size.height), Offset(corner, size.height)),
                            Triple(
                                Offset(size.width - corner, size.height),
                                Offset(size.width, size.height),
                                Offset(size.width, size.height - corner),
                            ),
                        )
                        for ((start, middle, end) in corners) {
                            drawLine(fitIconColor, start, middle, strokeWidth = strokeWidth)
                            drawLine(fitIconColor, middle, end, strokeWidth = strokeWidth)
                        }
                    }
                    Text(fitPinsLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private suspend fun fitPins(mapState: MapState, pins: List<BikePin>) {
    if (pins.isEmpty()) return
    val points = pins.map { WebMercator.toNormalized(it.location) }
    if (points.size == 1) {
        mapState.scrollTo(points.first().x, points.first().y, destScale = scaleForZoom(FOCUS_ZOOM_LEVEL))
        return
    }
    val box = BoundingBox(
        xLeft = points.minOf { it.x },
        yTop = points.minOf { it.y },
        xRight = points.maxOf { it.x },
        yBottom = points.maxOf { it.y },
    )
    mapState.scrollTo(area = box, padding = Offset(FIT_PADDING_RATIO, FIT_PADDING_RATIO))
}

private fun closedPathData(mapState: MapState, outline: List<GeoPoint>): PathData? {
    if (outline.isEmpty()) return null
    val builder = mapState.makePathDataBuilder()
    for (point in outline + outline.first()) {
        val normalized = WebMercator.toNormalized(point)
        builder.addPoint(normalized.x, normalized.y)
    }
    return builder.build()
}

private fun addParking(mapState: MapState, parking: Parking) {
    val radius = parking.radiusMeters
    val outline = if (radius != null && radius > 0) circleOutline(parking.center, radius) else parking.outline
    val color = if (parking.isActive) MapColors.parking else MapColors.parkingInactive
    val pathData = closedPathData(mapState, outline)
    if (pathData != null) {
        mapState.addPath(
            id = PARKING_PATH_PREFIX + parking.id,
            pathData = pathData,
            width = ParkingStrokeWidth,
            color = color,
            fillColor = color.copy(alpha = PARKING_FILL_ALPHA),
            pattern = listOf(PatternItem.Dash(ParkingDash), PatternItem.Gap(ParkingDash)),
        )
    }
    val center = WebMercator.toNormalized(parking.center)
    mapState.addMarker(
        id = PARKING_MARKER_PREFIX + parking.id,
        x = center.x,
        y = center.y,
        relativeOffset = CenteredRelativeOffset,
        zIndex = PARKING_Z_INDEX,
        clickable = false,
    ) { ParkingMarker(isActive = parking.isActive) }
}

private fun removeParking(mapState: MapState, parkingId: String) {
    mapState.removePath(PARKING_PATH_PREFIX + parkingId)
    mapState.removeMarker(PARKING_MARKER_PREFIX + parkingId)
}

private fun addRestrictedZone(mapState: MapState, zone: RestrictedZone) {
    val pathData = closedPathData(mapState, zone.outline) ?: return
    mapState.addPath(
        id = RESTRICTED_PATH_PREFIX + zone.id,
        pathData = pathData,
        width = RestrictedStrokeWidth,
        color = MapColors.restrictedZone,
        fillColor = MapColors.restrictedZone.copy(alpha = RESTRICTED_FILL_ALPHA),
    )
    val center = WebMercator.toNormalized(zone.center)
    mapState.addMarker(
        id = SIGN_MARKER_PREFIX + zone.id,
        x = center.x,
        y = center.y,
        relativeOffset = CenteredRelativeOffset,
        zIndex = if (zone.isNoRiding) NO_RIDING_SIGN_Z_INDEX else NO_PARKING_SIGN_Z_INDEX,
        clickable = false,
    ) { ZoneSignMarker(isNoRiding = zone.isNoRiding) }
}

private fun removeRestrictedZone(mapState: MapState, zoneId: String) {
    mapState.removePath(RESTRICTED_PATH_PREFIX + zoneId)
    mapState.removeMarker(SIGN_MARKER_PREFIX + zoneId)
}

private fun scaleForZoom(zoomLevel: Int): Double = 2.0.pow(zoomLevel - MAX_ZOOM_LEVEL)
