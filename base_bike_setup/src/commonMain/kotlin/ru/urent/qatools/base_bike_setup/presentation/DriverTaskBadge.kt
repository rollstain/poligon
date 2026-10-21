package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

private const val CAR_ICON_PATH = "M3 15V9.5L5.5 5H18.5L21 9.5V15M3 15H21"
private const val CAR_ICON_VIEWPORT = 24f
private const val CAR_ICON_STROKE = 2.4f
private const val CAR_WHEEL_RADIUS = 2f
private const val BADGE_ICON_RATIO = 0.6f
private const val BADGE_OUTLINE_DP = 2f
private val CarWheelCenters = listOf(Offset(7.5f, 17.5f), Offset(16.5f, 17.5f))
private val BadgeOutlineColor = Color(0xFFFFFFFF)
private val BadgeIconColor = Color(0xFFFFFFFF)

@Composable
fun DriverTaskBadge(color: Color, size: Dp, hasOutline: Boolean, modifier: Modifier = Modifier) {
    val car = remember { PathParser().parsePathString(CAR_ICON_PATH).toPath() }
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2
        drawCircle(color = color, radius = radius)
        if (hasOutline) {
            val outlineWidth = BADGE_OUTLINE_DP * density
            drawCircle(color = BadgeOutlineColor, radius = radius - outlineWidth / 2, style = Stroke(width = outlineWidth))
        }
        val iconSize = this.size.minDimension * BADGE_ICON_RATIO
        val iconScale = iconSize / CAR_ICON_VIEWPORT
        val iconOffset = (this.size.minDimension - iconSize) / 2
        withTransform({
            translate(left = iconOffset, top = iconOffset)
            scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
        }) {
            val stroke = Stroke(width = CAR_ICON_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(path = car, color = BadgeIconColor, style = stroke)
            for (wheel in CarWheelCenters) {
                drawCircle(color = BadgeIconColor, radius = CAR_WHEEL_RADIUS, center = wheel, style = stroke)
            }
        }
    }
}
