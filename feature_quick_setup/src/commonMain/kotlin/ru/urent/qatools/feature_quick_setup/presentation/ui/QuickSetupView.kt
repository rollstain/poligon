package ru.urent.qatools.feature_quick_setup.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.domain.PlacementCalculator
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.presentation.AvailabilityField
import ru.urent.qatools.base_bike_setup.presentation.ChargeTargetPicker
import ru.urent.qatools.base_bike_setup.presentation.StandFacts
import ru.urent.qatools.base_bike_setup.presentation.StatusLineView
import ru.urent.qatools.base_bike_setup.presentation.applyStatusLines
import ru.urent.qatools.base_bike_setup.presentation.chargeTargetColor
import ru.urent.qatools.base_bike_setup.presentation.driverTaskColor
import ru.urent.qatools.base_bike_setup.presentation.formatNumber
import ru.urent.qatools.base_bike_setup.presentation.lookupLine
import ru.urent.qatools.base_bike_setup.presentation.placementLine
import ru.urent.qatools.base_map.BikeMap
import ru.urent.qatools.base_map.BikePin
import ru.urent.qatools.base_map.MapLegend
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.base_map.PlaceSearchField
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupEvent
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaButtonStyle
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val QUICK_PIN_ID = "quick-bike"
private const val MAP_LOADER_SCRIM_ALPHA = 0.7f
private const val MAP_HINT_SCRIM_ALPHA = 0.82f
private val TargetColumnWidth = 380.dp
private val SimulatorIdFieldWidth = 200.dp
private val BikeTitleWidth = 180.dp
private val SearchFieldWidth = 340.dp

@Composable
fun QuickSetupView(state: QuickSetupState, tileLoader: MapTileLoader, onEvent: (QuickSetupEvent) -> Unit) {
    val found = state.lookup as? BikeLookup.Found
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = QaDimens.screenPaddingHorizontal, vertical = QaDimens.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
            Text("Разовая настройка", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.alignByBaseline())
            Text(
                "Один байк прямо сейчас. Понравилось — добавьте в шаблон.",
                style = MaterialTheme.typography.bodyMedium,
                color = QaColors.textSecondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
        StandStrip(state = state, found = found, onEvent = onEvent)
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL)) {
            TargetColumn(state = state, found = found, onEvent = onEvent, modifier = Modifier.width(TargetColumnWidth).fillMaxHeight())
            MapCard(state = state, found = found, tileLoader = tileLoader, onEvent = onEvent, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun StandStrip(state: QuickSetupState, found: BikeLookup.Found?, onEvent: (QuickSetupEvent) -> Unit) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
        ) {
            QaTextField(
                value = state.simulatorIdText,
                onValueChange = { text -> onEvent(QuickSetupEvent.OnSimulatorIdChanged(text)) },
                label = "Номер в симуляторе",
                placeholder = "например 4071",
                modifier = Modifier.width(SimulatorIdFieldWidth),
            )
            if (found == null) {
                val line = lookupLine(lookup = state.lookup, isLookingUp = state.isLookingUp)
                if (line == null) {
                    Text("Номер байка в симуляторе — S.4071 это 4071", color = QaColors.textSecondary, modifier = Modifier.weight(1f))
                } else {
                    StatusLineView(line, Modifier.weight(1f))
                }
            } else {
                val bike = found.bike
                Column(modifier = Modifier.width(BikeTitleWidth), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                    Text(bike.identifier, style = MaterialTheme.typography.titleMedium)
                    val cityName = bike.cityName
                    Text(
                        if (cityName == null) bike.modelName else "${bike.modelName}, $cityName",
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                    )
                }
                StandFacts(bike = bike, modifier = Modifier.weight(1f))
                TextButton(onClick = { onEvent(QuickSetupEvent.OnRefreshBikeClicked) }, enabled = !state.isRefreshingBike) {
                    Text(if (state.isRefreshingBike) "Обновляем…" else "Обновить")
                }
                QaButton(text = "QR-код", onClick = { onEvent(QuickSetupEvent.OnQrCodeClicked(bike)) }, style = QaButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun TargetColumn(state: QuickSetupState, found: BikeLookup.Found?, onEvent: (QuickSetupEvent) -> Unit, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(QaDimens.spaceL),
                verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXl),
            ) {
                Text("Станет после применения", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    Text("Заряд после применения", style = MaterialTheme.typography.titleSmall)
                    ChargeTargetPicker(
                        target = state.target,
                        onTargetChange = { target -> onEvent(QuickSetupEvent.OnTargetChanged(target)) },
                        thresholds = found?.bike?.thresholds,
                        currentPercent = found?.bike?.chargePercent,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    AvailabilityField(
                        target = state.availability,
                        reasons = state.reasons,
                        onTargetChange = { availability -> onEvent(QuickSetupEvent.OnAvailabilityChanged(availability)) },
                        label = "Доступ после применения",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val thresholds = found?.bike?.thresholds
                    val disableThresholdText = if (thresholds == null) "" else " (${formatNumber(thresholds.disablePercent)}% у ${found.bike.modelName})"
                    Text(
                        text = if (state.availability == AvailabilityTarget.Available) {
                            "Меняется, как только трекер подтвердит заряд. Если заряд ниже порога снятия$disableThresholdText, " +
                                "стенд через 5–15 минут сам снимет байк с «Низкий заряд батареи» и создаст задачу на замену АКБ"
                        } else {
                            "Меняется, как только трекер подтвердит заряд — обычно за секунды"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    Text("Место", style = MaterialTheme.typography.titleSmall)
                    QaTextField(
                        value = state.coordinatesText,
                        onValueChange = { text -> onEvent(QuickSetupEvent.OnCoordinatesTextChanged(text)) },
                        label = "Координаты пина",
                        placeholder = "широта, долгота",
                        errorText = if (state.isCoordinatesInvalid) "Формат: широта, долгота" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val location = state.location
                    val hasZones = state.zones.parkings.isNotEmpty() || state.zones.rentZones.isNotEmpty()
                    if (location != null && (hasZones || state.techZones.isNotEmpty())) {
                        val placement = PlacementCalculator.placementOf(location, state.zones)
                        val techZoneName = state.techZones.firstOrNull { PlacementCalculator.isInsidePolygon(location, it.outline) }?.name
                        StatusLineView(placementLine(placement, hasRentZones = state.zones.rentZones.isNotEmpty(), techZoneName = techZoneName))
                    }
                }
            }
            HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
            Column(modifier = Modifier.padding(QaDimens.spaceL), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                for (line in applyStatusLines(state.status)) StatusLineView(line)
                val message = state.message
                if (message != null) {
                    Text(message, color = QaColors.positiveText)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    QaButton(
                        text = if (state.isApplying) "Применяется…" else "Применить сейчас",
                        onClick = { onEvent(QuickSetupEvent.OnApplyClicked) },
                        enabled = found != null && state.location != null && !state.isApplying,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        var isTemplateMenuOpen by remember { mutableStateOf(false) }
                        QaButton(
                            text = "В шаблон…",
                            onClick = { isTemplateMenuOpen = true },
                            style = QaButtonStyle.Secondary,
                            enabled = found != null && state.location != null && state.templates.isNotEmpty(),
                        )
                        DropdownMenu(expanded = isTemplateMenuOpen, onDismissRequest = { isTemplateMenuOpen = false }) {
                            for (template in state.templates) {
                                DropdownMenuItem(
                                    text = { Text(template.name) },
                                    onClick = {
                                        isTemplateMenuOpen = false
                                        onEvent(QuickSetupEvent.OnAddToTemplateClicked(template))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapCard(
    state: QuickSetupState,
    found: BikeLookup.Found?,
    tileLoader: MapTileLoader,
    onEvent: (QuickSetupEvent) -> Unit,
    modifier: Modifier,
) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = modifier) {
        Column(modifier = Modifier.padding(QaDimens.spaceM), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            val tileSource = state.tileSource
            val mapCenter = state.mapCenter
            val location = state.location
            val task = found?.bike?.status?.driverTask
            when {
                tileSource == null -> Text(
                    "На стенде нет настроек карты 2GIS — задайте место координатами",
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(QaDimens.spaceM),
                )
                mapCenter == null -> Column(modifier = Modifier.padding(QaDimens.spaceM), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    PlaceSearchField(
                        cityName = state.city?.name,
                        cities = state.cities,
                        techZones = state.techZones,
                        onCitySelected = { city -> onEvent(QuickSetupEvent.OnCitySelected(city)) },
                        onTechZoneSelected = { techZone -> onEvent(QuickSetupEvent.OnTechZoneSelected(techZone)) },
                        onPointSelected = { point -> onEvent(QuickSetupEvent.OnPointSelected(point)) },
                        modifier = Modifier.width(SearchFieldWidth),
                    )
                    Text("Выберите город или введите номер байка — карта откроется на месте", color = QaColors.textSecondary)
                }
                else -> Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(QaDimens.radiusButton))) {
                    BikeMap(
                        tileSource = tileSource,
                        tileLoader = tileLoader,
                        initialCenter = mapCenter,
                        focus = state.focus,
                        pins = if (found == null || location == null) {
                            emptyList()
                        } else {
                            listOf(
                                BikePin(
                                    id = QUICK_PIN_ID,
                                    label = found.bike.simulatorId.toString(),
                                    location = location,
                                    color = chargeTargetColor(state.target),
                                    isSelected = true,
                                    badgeColor = if (task == null) null else driverTaskColor(task),
                                ),
                            )
                        },
                        zones = state.zones,
                        techZones = state.techZones,
                        onMapClick = { clicked -> onEvent(QuickSetupEvent.OnMapClicked(clicked)) },
                        onPinMoved = { pinId, moved -> onEvent(QuickSetupEvent.OnPinMoved(pinId, moved)) },
                        onPinClick = {},
                        onParkingAreaIdle = { center -> onEvent(QuickSetupEvent.OnParkingAreaIdle(center)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    PlaceSearchField(
                        cityName = state.city?.name,
                        cities = state.cities,
                        techZones = state.techZones,
                        onCitySelected = { city -> onEvent(QuickSetupEvent.OnCitySelected(city)) },
                        onTechZoneSelected = { techZone -> onEvent(QuickSetupEvent.OnTechZoneSelected(techZone)) },
                        onPointSelected = { point -> onEvent(QuickSetupEvent.OnPointSelected(point)) },
                        modifier = Modifier.align(Alignment.TopStart).padding(QaDimens.spaceM).width(SearchFieldWidth),
                    )
                    MapLegend(modifier = Modifier.align(Alignment.BottomStart).padding(QaDimens.spaceM))
                    if (found != null) {
                        Text(
                            "Перетащите пин или кликните по карте",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QaColors.surface,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(QaDimens.spaceM)
                                .background(QaColors.textPrimary.copy(alpha = MAP_HINT_SCRIM_ALPHA), RoundedCornerShape(QaDimens.radiusField))
                                .padding(horizontal = QaDimens.spaceM, vertical = QaDimens.spaceS),
                        )
                    }
                    if (state.isLookingUp) {
                        Column(
                            modifier = Modifier
                                .matchParentSize()
                                .background(QaColors.surface.copy(alpha = MAP_LOADER_SCRIM_ALPHA)),
                            verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = QaColors.brand)
                            Text("Ищем байк на стенде…", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            if (state.isZonesFailed) {
                Text("Не удалось загрузить парковки и зоны", color = QaColors.negative, modifier = Modifier.padding(horizontal = QaDimens.spaceS))
            }
        }
    }
}
