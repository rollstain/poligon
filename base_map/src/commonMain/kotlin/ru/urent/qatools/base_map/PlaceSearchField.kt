package ru.urent.qatools.base_map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.uikit.components.Suggestion
import ru.urent.qatools.uikit.components.SuggestionField
import ru.urent.qatools.uikit.components.SuggestionGroup
import ru.urent.qatools.base_bike_setup.presentation.formatCoordinates
import ru.urent.qatools.uikit.components.matchesQuery
import ru.urent.qatools.base_bike_setup.presentation.parseCoordinates
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

internal sealed interface MapPlace {
    data class CityPlace(val city: City) : MapPlace
    data class TechZonePlace(val techZone: TechZone) : MapPlace
    data class PointPlace(val point: GeoPoint) : MapPlace
}

private const val MAX_SUGGESTIONS_PER_GROUP = 8
private const val SEARCH_GLYPH_STROKE_DP = 2f
private const val SEARCH_GLYPH_RADIUS_RATIO = 0.32f
private const val SEARCH_GLYPH_CENTER_RATIO = 0.42f
private const val COS_45_DEGREES = 0.7071f
private const val PLACE_ICON_STROKE_DP = 1.5f
private const val PLACE_ICON_DASH_DP = 3f
private const val PLACE_ICON_INSET_RATIO = 0.15f
private const val PLACE_ICON_DOT_RATIO = 0.25f
private val SearchGlyphSize = 18.dp
private val PlaceIconSize = 18.dp
private val CityChipShape = RoundedCornerShape(8.dp)

@Composable
fun PlaceSearchField(
    cityName: String?,
    cities: List<City>,
    techZones: List<TechZone>,
    onCitySelected: (City) -> Unit,
    onTechZoneSelected: (TechZone) -> Unit,
    onPointSelected: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    SuggestionField(
        selectedText = "",
        label = "Найти на карте",
        placeholder = "Город, техзона или координаты",
        groupsFor = { query -> placeSuggestionGroups(query, cityName, cities, techZones) },
        onSelect = { place ->
            when (place) {
                is MapPlace.CityPlace -> onCitySelected(place.city)
                is MapPlace.TechZonePlace -> onTechZoneSelected(place.techZone)
                is MapPlace.PointPlace -> onPointSelected(place.point)
            }
        },
        suggestionIcon = { place ->
            val cityIconColor = QaColors.textSecondary
            val pointIconColor = QaColors.brand
            Canvas(modifier = Modifier.size(PlaceIconSize)) {
                val strokeWidth = PLACE_ICON_STROKE_DP * density
                val inset = size.minDimension * PLACE_ICON_INSET_RATIO
                when (place) {
                    is MapPlace.TechZonePlace -> drawRect(
                        color = MapColors.techZone,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(PLACE_ICON_DASH_DP * density, PLACE_ICON_DASH_DP * density)),
                        ),
                    )
                    is MapPlace.CityPlace -> drawCircle(
                        color = cityIconColor,
                        radius = size.minDimension / 2 - inset,
                        style = Stroke(width = strokeWidth),
                    )
                    is MapPlace.PointPlace -> drawCircle(color = pointIconColor, radius = size.minDimension * PLACE_ICON_DOT_RATIO)
                }
            }
        },
        emptyText = if (cities.isEmpty()) "Список городов не загрузился со стенда" else "Ничего не нашлось",
        leadingIcon = {
            val glyphColor = QaColors.textSecondary
            Canvas(modifier = Modifier.size(SearchGlyphSize)) {
                val strokeWidth = SEARCH_GLYPH_STROKE_DP * density
                val center = Offset(size.width * SEARCH_GLYPH_CENTER_RATIO, size.height * SEARCH_GLYPH_CENTER_RATIO)
                val radius = size.minDimension * SEARCH_GLYPH_RADIUS_RATIO
                drawCircle(color = glyphColor, radius = radius, center = center, style = Stroke(width = strokeWidth))
                drawLine(
                    color = glyphColor,
                    start = center + Offset(radius * COS_45_DEGREES, radius * COS_45_DEGREES),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        },
        trailingIcon = if (cityName == null) {
            null
        } else {
            {
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.labelMedium,
                    color = QaColors.brandText,
                    modifier = Modifier
                        .padding(end = QaDimens.spaceS)
                        .background(QaColors.brandSelection, CityChipShape)
                        .padding(horizontal = QaDimens.spaceS, vertical = QaDimens.spaceXs),
                )
            }
        },
        modifier = modifier,
    )
}

internal fun placeSuggestionGroups(
    query: String,
    cityName: String?,
    cities: List<City>,
    techZones: List<TechZone>,
): List<SuggestionGroup<MapPlace>> {
    val point = parseCoordinates(query)
    if (point != null) {
        val pointSuggestion = Suggestion<MapPlace>(MapPlace.PointPlace(point), "Перейти к ${formatCoordinates(point)}", null)
        return listOf(SuggestionGroup(title = null, suggestions = listOf(pointSuggestion), hiddenCount = 0))
    }
    val matchingTechZones = techZones.filter { matchesQuery(it.name, query) }
    val matchingCities = cities.filter { matchesQuery(it.name, query) }
    val techZoneSuggestions = matchingTechZones.take(MAX_SUGGESTIONS_PER_GROUP).map { techZone ->
        Suggestion<MapPlace>(MapPlace.TechZonePlace(techZone), techZone.name, null)
    }
    val citySuggestions = matchingCities.take(MAX_SUGGESTIONS_PER_GROUP).map { city ->
        Suggestion<MapPlace>(MapPlace.CityPlace(city), city.name, if (city.name == cityName) "открыт сейчас" else null)
    }
    return listOf(
        SuggestionGroup(
            title = if (cityName == null) "Техзоны" else "Техзоны · $cityName",
            suggestions = techZoneSuggestions,
            hiddenCount = matchingTechZones.size - techZoneSuggestions.size,
        ),
        SuggestionGroup(
            title = "Города",
            suggestions = citySuggestions,
            hiddenCount = matchingCities.size - citySuggestions.size,
        ),
    )
}
