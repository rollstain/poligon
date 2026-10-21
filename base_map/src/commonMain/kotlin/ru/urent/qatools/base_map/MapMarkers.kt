package ru.urent.qatools.base_map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.urent.qatools.base_bike_setup.presentation.DriverTaskBadge

private const val PIN_WIDE_PATH = "M36 0C44.8366 0 52 7.16344 52 16V22C52 30.8366 44.8366 38 36 38H31.9334C30.5544 38 " +
    "29.353 38.9401 29.0214 40.2786L28.912 40.7205C28.1593 43.7586 23.8407 43.7586 23.088 40.7205L22.9786 40.2786C22.647 " +
    "38.9401 21.4456 38 20.0666 38H16C7.1634 38 0 30.8366 0 22V16C0 7.16344 7.1634 0 16 0H36Z"
private const val PIN_ACTIVE_PATH = "M38 0C51.2548 0 62 10.7452 62 24V36.9727C61.9999 50.2274 51.2547 60.9727 38 " +
    "60.9727H37.4711C35.944 60.9727 34.6116 62.0088 34.2353 63.4888C33.3823 66.8435 28.6164 66.8436 27.7636 " +
    "63.4888C27.3874 62.0088 26.0549 60.9727 24.5278 60.9727H24C10.7453 60.9727 0.0001 50.2274 0 36.9727V24C0 " +
    "10.7452 10.7452 0 24 0H38Z"
private const val PARKING_ICON_PATH = "M4.2 3.5C4.2 2.7268 4.8268 2.1 5.6 2.1H7.23333C9.03746 2.1 10.5 3.56254 10.5 " +
    "5.36667C10.5 7.1708 9.03746 8.63333 7.23333 8.63333H5.6V11.2C5.6 11.5866 5.2866 11.9 4.9 11.9C4.5134 11.9 4.2 " +
    "11.5866 4.2 11.2V3.5ZM5.6 7.23333H7.23333C8.26427 7.23333 9.1 6.3976 9.1 5.36667C9.1 4.33574 8.26427 3.5 " +
    "7.23333 3.5H5.6V7.23333Z"
private const val NO_RIDING_ICON_PATH = "M7 12H21C21.5523 12 22 12.4477 22 13V15C22 15.5523 21.5523 16 21 16H7C6.44772 " +
    "16 6 15.5523 6 15V13C6 12.4477 6.44772 12 7 12Z"
private const val NO_PARKING_ICON_PATH = "M12.741 15.29V18.988H11.012V9.035H14.819Q16.487 9.035 17.468 9.903Q18.449 " +
    "10.771 18.449 12.2Q18.449 13.663 17.489 14.477Q16.528 15.29 14.799 15.29ZM12.741 13.902H14.819Q15.742 13.902 " +
    "16.228 13.468Q16.713 13.034 16.713 12.214Q16.713 11.407 16.221 10.925Q15.729 10.443 14.867 10.43H12.741Z"
private const val NO_PARKING_STRIKE_PATH = "M4 5L23 24"
private const val PIN_BADGE_SIZE_DP = 22f
private const val PIN_BADGE_OVERHANG_DP = 8f
private const val TECH_ZONE_LABEL_CORNER_DP = 8f
private const val TECH_ZONE_LABEL_PADDING_DP = 8f
private const val TECH_ZONE_LABEL_SIZE_SP = 12f

private const val PIN_STROKE_DP = 2f
private const val ANCHOR_DOT_STROKE_DP = 1f
private const val PIN_SHADOW_OFFSET_DP = 1f
private const val LIGHT_PIN_LUMINANCE = 0.5f
private const val PARKING_PIN_SIZE_DP = 22f
private const val PARKING_PIN_CORNER_DP = 8f
private const val PARKING_ICON_INSET_DP = 4f
private const val SIGN_SIZE_DP = 28f
private const val SIGN_STROKE_DP = 1f

private data class PinGeometry(
    val pathData: String,
    val widthDp: Float,
    val heightDp: Float,
    val bodyHeightDp: Float,
    val anchorYDp: Float,
    val anchorDotRadiusDp: Float,
    val labelSizeSp: Float,
)

private val WidePinGeometry = PinGeometry(
    pathData = PIN_WIDE_PATH,
    widthDp = 52f,
    heightDp = 52f,
    bodyHeightDp = 38f,
    anchorYDp = 48f,
    anchorDotRadiusDp = 2.5f,
    labelSizeSp = 14f,
)

private val ActivePinGeometry = PinGeometry(
    pathData = PIN_ACTIVE_PATH,
    widthDp = 62f,
    heightDp = 77f,
    bodyHeightDp = 61f,
    anchorYDp = 73f,
    anchorDotRadiusDp = 3f,
    labelSizeSp = 17f,
)

internal fun pinAnchorOffset(isSelected: Boolean): DpOffset {
    val geometry = if (isSelected) ActivePinGeometry else WidePinGeometry
    return DpOffset(x = 0.dp, y = -geometry.anchorYDp.dp)
}

@Composable
internal fun TechZoneLabel(name: String) {
    Text(
        text = name,
        modifier = Modifier
            .background(MapColors.techZoneLabelBackground, RoundedCornerShape(TECH_ZONE_LABEL_CORNER_DP.dp))
            .padding(horizontal = TECH_ZONE_LABEL_PADDING_DP.dp),
        color = MapColors.techZoneLabel,
        fontSize = TECH_ZONE_LABEL_SIZE_SP.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
internal fun BikePinMarker(label: String, color: Color, isSelected: Boolean, badgeColor: Color?) {
    val geometry = if (isSelected) ActivePinGeometry else WidePinGeometry
    val body = remember(geometry) { PathParser().parsePathString(geometry.pathData).toPath() }
    val labelColor = if (color.luminance() > LIGHT_PIN_LUMINANCE) MapColors.pinDarkLabel else MapColors.pinLightLabel
    Box(modifier = Modifier.size(width = geometry.widthDp.dp, height = geometry.heightDp.dp)) {
        Canvas(modifier = Modifier.size(width = geometry.widthDp.dp, height = geometry.heightDp.dp)) {
            val pixelsPerDp = density
            withTransform({ scale(scaleX = pixelsPerDp, scaleY = pixelsPerDp, pivot = Offset.Zero) }) {
                translate(top = PIN_SHADOW_OFFSET_DP) { drawPath(path = body, color = MapColors.pinShadow) }
                drawPath(path = body, color = color)
                drawPath(path = body, color = MapColors.pinStroke, style = Stroke(width = PIN_STROKE_DP))
                val anchor = Offset(x = geometry.widthDp / 2, y = geometry.anchorYDp)
                drawCircle(color = MapColors.anchorDot, radius = geometry.anchorDotRadiusDp, center = anchor)
                drawCircle(
                    color = MapColors.pinStroke,
                    radius = geometry.anchorDotRadiusDp,
                    center = anchor,
                    style = Stroke(width = ANCHOR_DOT_STROKE_DP),
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier
                .width(geometry.widthDp.dp)
                .height(geometry.bodyHeightDp.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            color = labelColor,
            fontSize = geometry.labelSizeSp.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (badgeColor != null) {
            DriverTaskBadge(
                color = badgeColor,
                size = PIN_BADGE_SIZE_DP.dp,
                hasOutline = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = PIN_BADGE_OVERHANG_DP.dp, y = -PIN_BADGE_OVERHANG_DP.dp),
            )
        }
    }
}

@Composable
internal fun ParkingMarker(isActive: Boolean) {
    val color = if (isActive) MapColors.parking else MapColors.parkingInactive
    val shape = RoundedCornerShape(PARKING_PIN_CORNER_DP.dp)
    val icon = remember {
        val path = PathParser().parsePathString(PARKING_ICON_PATH).toPath()
        path.fillType = PathFillType.EvenOdd
        path.translate(Offset(PARKING_ICON_INSET_DP, PARKING_ICON_INSET_DP))
        path
    }
    Canvas(
        modifier = Modifier
            .size(PARKING_PIN_SIZE_DP.dp)
            .background(color = color, shape = shape)
            .border(width = PIN_STROKE_DP.dp, color = MapColors.pinStroke, shape = shape),
    ) {
        val pixelsPerDp = density
        withTransform({ scale(scaleX = pixelsPerDp, scaleY = pixelsPerDp, pivot = Offset.Zero) }) {
            drawPath(path = icon, color = MapColors.pinLightLabel)
        }
    }
}

@Composable
internal fun ZoneSignMarker(isNoRiding: Boolean) {
    val icon = remember(isNoRiding) {
        PathParser().parsePathString(if (isNoRiding) NO_RIDING_ICON_PATH else NO_PARKING_ICON_PATH).toPath()
    }
    val strike = remember { PathParser().parsePathString(NO_PARKING_STRIKE_PATH).toPath() }
    Canvas(modifier = Modifier.size(SIGN_SIZE_DP.dp)) {
        val pixelsPerDp = density
        withTransform({ scale(scaleX = pixelsPerDp, scaleY = pixelsPerDp, pivot = Offset.Zero) }) {
            val center = Offset(SIGN_SIZE_DP / 2, SIGN_SIZE_DP / 2)
            val radius = SIGN_SIZE_DP / 2
            val fill = if (isNoRiding) MapColors.restrictedZone else MapColors.signSurface
            val outline = if (isNoRiding) MapColors.signSurface else MapColors.restrictedZone
            drawCircle(color = fill, radius = radius, center = center)
            drawCircle(color = outline, radius = radius, center = center, style = Stroke(width = SIGN_STROKE_DP))
            if (isNoRiding) {
                drawPath(path = icon, color = MapColors.signSurface)
            } else {
                drawPath(path = icon, color = MapColors.pinDarkLabel)
                drawPath(path = strike, color = MapColors.restrictedZone, style = Stroke(width = SIGN_STROKE_DP))
            }
        }
    }
}

@Composable
internal fun BikePinLegendIcon(color: Color) {
    val body = remember { PathParser().parsePathString(PIN_WIDE_PATH).toPath() }
    Canvas(modifier = Modifier.size(width = (WidePinGeometry.widthDp / 2).dp, height = (WidePinGeometry.heightDp / 2).dp)) {
        val pixelsPerHalfDp = density / 2
        withTransform({ scale(scaleX = pixelsPerHalfDp, scaleY = pixelsPerHalfDp, pivot = Offset.Zero) }) {
            drawPath(path = body, color = color)
            drawCircle(
                color = MapColors.anchorDot,
                radius = WidePinGeometry.anchorDotRadiusDp * 2,
                center = Offset(WidePinGeometry.widthDp / 2, WidePinGeometry.anchorYDp),
            )
        }
    }
}
