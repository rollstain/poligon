package ru.urent.qatools.feature_templates.presentation.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.domain.ChargeCalculator
import ru.urent.qatools.base_bike_setup.domain.PlacementCalculator
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.presentation.AvailabilityField
import ru.urent.qatools.base_bike_setup.presentation.ChargeScaleBar
import ru.urent.qatools.base_bike_setup.presentation.ChargeTargetPicker
import ru.urent.qatools.base_bike_setup.presentation.StandStateLines
import ru.urent.qatools.base_bike_setup.presentation.StatusLineView
import ru.urent.qatools.base_bike_setup.presentation.availabilityTargetLabel
import ru.urent.qatools.base_bike_setup.presentation.bikeResultLine
import ru.urent.qatools.base_bike_setup.presentation.chargeStateShortLabel
import ru.urent.qatools.base_bike_setup.presentation.chargeTargetColor
import ru.urent.qatools.base_bike_setup.presentation.driverTaskColor
import ru.urent.qatools.base_bike_setup.presentation.failureText
import ru.urent.qatools.base_bike_setup.presentation.formatPercent
import ru.urent.qatools.base_bike_setup.presentation.lookupLine
import ru.urent.qatools.base_bike_setup.presentation.templateStatusLine
import ru.urent.qatools.base_map.BikeMap
import ru.urent.qatools.base_map.BikePin
import ru.urent.qatools.base_map.MapLegend
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.base_map.PlaceSearchField
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateBikeRow
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateEvent
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaButtonStyle
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.components.Suggestion
import ru.urent.qatools.uikit.components.SuggestionField
import ru.urent.qatools.uikit.components.SuggestionGroup
import ru.urent.qatools.uikit.components.matchesQuery
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val TECH_ZONES_SHOWN_PER_CITY_WITHOUT_QUERY = 6
private val BikeColumnWidth = 360.dp
private val ChargeScaleBarWidth = 96.dp
private val SearchFieldWidth = 340.dp
private val TechZoneMenuWidth = 328.dp
private val MenuButtonSize = 44.dp

@Composable
fun TemplateView(state: TemplateState, tileLoader: MapTileLoader, onEvent: (TemplateEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = QaDimens.screenPaddingHorizontal, vertical = QaDimens.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
            Text(
                text = "←",
                style = MaterialTheme.typography.headlineSmall,
                color = QaColors.brandText,
                modifier = Modifier
                    .clip(RoundedCornerShape(QaDimens.radiusField))
                    .clickable { onEvent(TemplateEvent.OnBackClicked) }
                    .padding(horizontal = QaDimens.spaceS)
                    .semantics { contentDescription = "К шаблонам" },
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                val nameInteraction = remember { MutableInteractionSource() }
                val isNameFocused by nameInteraction.collectIsFocusedAsState()
                val isNameHovered by nameInteraction.collectIsHoveredAsState()
                BasicTextField(
                    value = state.name,
                    onValueChange = { name -> onEvent(TemplateEvent.OnNameChanged(name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = QaColors.textPrimary),
                    cursorBrush = SolidColor(QaColors.brand),
                    interactionSource = nameInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverable(nameInteraction)
                        .background(
                            color = if (isNameFocused || isNameHovered) QaColors.surface else Color.Transparent,
                            shape = RoundedCornerShape(QaDimens.radiusField),
                        )
                        .padding(horizontal = QaDimens.spaceS)
                        .semantics { contentDescription = "Название шаблона" },
                    decorationBox = { nameField ->
                        Box {
                            if (state.name.isEmpty()) {
                                Text("Название шаблона", style = MaterialTheme.typography.headlineSmall, color = QaColors.textSecondary)
                            }
                            nameField()
                        }
                    },
                )
                if (state.rows.isNotEmpty()) {
                    StatusLineView(
                        templateStatusLine(
                            bikeStatuses = state.rows.map { it.status },
                            isApplying = state.isApplying,
                            changedCount = state.rows.count { it.isChangedSinceApply },
                            appliedAt = state.lastAppliedAt,
                        ),
                        Modifier.padding(horizontal = QaDimens.spaceS),
                    )
                }
            }
            QaButton(
                text = if (state.isRefreshing) "Обновляем…" else "Обновить статусы",
                onClick = { onEvent(TemplateEvent.OnRefreshStatusesClicked) },
                style = QaButtonStyle.Secondary,
                enabled = !state.isRefreshing && state.rows.isNotEmpty(),
            )
            QaButton(
                text = if (state.isApplying) "Применяется…" else "Применить все (${state.rows.size})",
                onClick = { onEvent(TemplateEvent.OnApplyAllClicked) },
                enabled = !state.isApplying && state.rows.isNotEmpty(),
            )
            Box {
                var isMenuOpen by remember { mutableStateOf(false) }
                Surface(
                    onClick = { isMenuOpen = true },
                    shape = RoundedCornerShape(QaDimens.radiusField),
                    color = QaColors.surface,
                    modifier = Modifier.size(MenuButtonSize).semantics { contentDescription = "Ещё действия с шаблоном" },
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("⋯", style = MaterialTheme.typography.titleLarge) }
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }, containerColor = QaColors.surface) {
                    DropdownMenuItem(
                        text = { Text("Удалить шаблон", color = QaColors.negative) },
                        onClick = {
                            isMenuOpen = false
                            onEvent(TemplateEvent.OnDeleteClicked)
                        },
                    )
                }
            }
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL)) {
            Surface(
                shape = RoundedCornerShape(QaDimens.radiusCard),
                color = QaColors.surface,
                modifier = Modifier.width(BikeColumnWidth).fillMaxHeight(),
            ) {
                val selectedRow = state.rows.firstOrNull { it.bike.id == state.selectedBikeId }
                if (selectedRow == null) {
                    BikeList(state = state, onEvent = onEvent)
                } else {
                    BikeCard(row = selectedRow, state = state, onEvent = onEvent)
                }
            }
            MapCard(modifier = Modifier.weight(1f).fillMaxHeight(), state = state, tileLoader = tileLoader, onEvent = onEvent)
        }
    }

    if (state.isDeleteConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(TemplateEvent.OnDeleteDismissed) },
            containerColor = QaColors.surface,
            shape = RoundedCornerShape(QaDimens.radiusDialog),
            title = { Text("Удалить шаблон «${state.name}»?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Байки на стенде не изменятся, удалится только шаблон в приложении.") },
            confirmButton = { QaButton(text = "Удалить", onClick = { onEvent(TemplateEvent.OnDeleteConfirmed) }) },
            dismissButton = {
                QaButton(text = "Отмена", onClick = { onEvent(TemplateEvent.OnDeleteDismissed) }, style = QaButtonStyle.Secondary)
            },
        )
    }
}

@Composable
private fun BikeList(state: TemplateState, onEvent: (TemplateEvent) -> Unit) {
    val allTechZones = state.cityTechZones.flatMap { it.techZones }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Байки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${state.rows.size} в шаблоне", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (state.rows.isEmpty()) {
                HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                Text(
                    "В шаблоне пока нет байков — добавьте первый по номеру в симуляторе",
                    modifier = Modifier.padding(QaDimens.spaceL),
                    color = QaColors.textSecondary,
                )
            }
            for (row in state.rows) {
                HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                val setup = row.bike.setup
                val lookup = row.lookup
                val thresholds = (lookup as? BikeLookup.Found)?.bike?.thresholds
                val target = setup.target
                val chargeText = when (target) {
                    is ChargeTarget.Percent -> "Точный ${formatPercent(target.percent)}"
                    is ChargeTarget.State -> if (thresholds == null) {
                        chargeStateShortLabel(target.state)
                    } else {
                        "${chargeStateShortLabel(target.state)}, ${formatPercent(ChargeCalculator.targetPercent(target, thresholds))}"
                    }
                }
                val techZoneName = allTechZones.firstOrNull { PlacementCalculator.isInsidePolygon(setup.location, it.outline) }?.name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(TemplateEvent.OnBikeClicked(row.bike)) }
                        .padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM)
                        .semantics { contentDescription = "Байк S.${setup.simulatorId}" },
                    verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S.${setup.simulatorId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusLineView(bikeResultLine(row.status, row.isChangedSinceApply))
                    }
                    if (lookup is BikeLookup.Failed) {
                        Text(failureText(lookup.failure), style = MaterialTheme.typography.bodySmall, color = QaColors.negative)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
                        if (thresholds == null) {
                            Box(Modifier.size(QaDimens.chipDotSize).background(chargeTargetColor(target), RoundedCornerShape(QaDimens.chipDotSize)))
                        } else {
                            ChargeScaleBar(
                                percent = ChargeCalculator.targetPercent(target, thresholds),
                                thresholds = thresholds,
                                modifier = Modifier.width(ChargeScaleBarWidth),
                            )
                        }
                        Text(chargeText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
                        Text(
                            techZoneName ?: "Вне техзон",
                            style = MaterialTheme.typography.bodySmall,
                            color = QaColors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(availabilityTargetLabel(setup.availability), style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                    }
                }
            }
        }
        HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
        val newBike = state.newBike
        Column(modifier = Modifier.padding(QaDimens.spaceL), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                QaTextField(
                    value = newBike.simulatorIdText,
                    onValueChange = { text -> onEvent(TemplateEvent.OnNewBikeTextChanged(text)) },
                    label = "Добавить байк: номер в симуляторе",
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            val isEnter = event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            if (isEnter && newBike.canAdd) onEvent(TemplateEvent.OnAddNewBikeClicked)
                            isEnter
                        },
                )
                QaButton(text = "Добавить", onClick = { onEvent(TemplateEvent.OnAddNewBikeClicked) }, style = QaButtonStyle.Secondary, enabled = newBike.canAdd)
            }
            val line = lookupLine(lookup = newBike.lookup, isLookingUp = newBike.isLookingUp)
            if (line != null) StatusLineView(line)
        }
    }
}

@Composable
private fun BikeCard(row: TemplateBikeRow, state: TemplateState, onEvent: (TemplateEvent) -> Unit) {
    val bike = row.bike
    val setup = bike.setup
    val lookup = row.lookup
    val found = (lookup as? BikeLookup.Found)?.bike
    val allTechZones = state.cityTechZones.flatMap { it.techZones }
    val techZoneName = allTechZones.firstOrNull { PlacementCalculator.isInsidePolygon(setup.location, it.outline) }?.name
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(QaDimens.spaceL),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Text(
            text = "← Все байки (${state.rows.size})",
            style = MaterialTheme.typography.labelLarge,
            color = QaColors.brandText,
            modifier = Modifier
                .clip(RoundedCornerShape(QaDimens.radiusField))
                .clickable { onEvent(TemplateEvent.OnBikeCardClosed) }
                .padding(vertical = QaDimens.spaceXs),
        )
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                Text("S.${setup.simulatorId}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = when (lookup) {
                        null -> "Загружаем…"
                        is BikeLookup.Found -> lookup.bike.modelName
                        is BikeLookup.Failed -> failureText(lookup.failure)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lookup is BikeLookup.Failed) QaColors.negative else QaColors.textSecondary,
                )
            }
            StatusLineView(bikeResultLine(row.status, row.isChangedSinceApply))
        }
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            Text("Заряд после применения", style = MaterialTheme.typography.titleSmall)
            ChargeTargetPicker(
                target = setup.target,
                onTargetChange = { target -> onEvent(TemplateEvent.OnChargeChanged(bike, target)) },
                thresholds = found?.thresholds,
                currentPercent = found?.chargePercent,
            )
        }
        AvailabilityField(
            target = setup.availability,
            reasons = state.reasons,
            onTargetChange = { availability -> onEvent(TemplateEvent.OnAvailabilityChanged(bike, availability)) },
            label = "Доступ после применения",
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Доступ S.${setup.simulatorId}" },
        )
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            SuggestionField(
                selectedText = techZoneName ?: "Вне техзон",
                label = "Техзона",
                placeholder = "Техзона любого города",
                groupsFor = { query ->
                    val isQueryBlank = query.isBlank()
                    val groups = mutableListOf<SuggestionGroup<TechZone>>()
                    for (cityTechZones in state.cityTechZones) {
                        val matching = cityTechZones.techZones.filter { matchesQuery(it.name, query) }
                        if (matching.isEmpty()) continue
                        val shown = if (isQueryBlank) matching.take(TECH_ZONES_SHOWN_PER_CITY_WITHOUT_QUERY) else matching
                        val suggestions = shown.map { techZone -> Suggestion(value = techZone, title = techZone.name, supportingText = null) }
                        groups.add(SuggestionGroup(title = cityTechZones.city.name, suggestions = suggestions, hiddenCount = matching.size - shown.size))
                    }
                    groups
                },
                onSelect = { techZone -> onEvent(TemplateEvent.OnBikeTechZoneSelected(bikeId = bike.id, techZone = techZone)) },
                suggestionIcon = {},
                emptyText = if (state.cityTechZones.isEmpty()) "Техзоны стенда не загрузились" else "Такой техзоны нет",
                menuWidth = TechZoneMenuWidth,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Техзона S.${setup.simulatorId}" },
            )
            Text(
                "Байк встанет в центр техзоны. Точнее — перетащите пин на карте",
                style = MaterialTheme.typography.bodySmall,
                color = QaColors.textSecondary,
            )
        }
        if (found != null) {
            StandStateLines(found)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            if (found != null) {
                QaButton(
                    text = "QR-код",
                    onClick = { onEvent(TemplateEvent.OnQrCodeClicked(found)) },
                    style = QaButtonStyle.Secondary,
                    modifier = Modifier.semantics { contentDescription = "QR-код ${found.identifier}" },
                )
            }
            TextButton(onClick = { onEvent(TemplateEvent.OnRemoveBikeClicked(bike.id)) }) {
                Text("Убрать из шаблона", color = QaColors.negative)
            }
        }
    }
}

@Composable
private fun MapCard(modifier: Modifier, state: TemplateState, tileLoader: MapTileLoader, onEvent: (TemplateEvent) -> Unit) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = modifier) {
        Column(modifier = Modifier.padding(QaDimens.spaceM), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            val tileSource = state.tileSource
            val firstLocation = state.rows.firstOrNull()?.bike?.setup?.location
            when {
                tileSource == null -> Text(
                    "На стенде нет настроек карты 2GIS — место байка можно задать координатами в разовой настройке",
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(QaDimens.spaceM),
                )
                firstLocation == null -> Text(
                    "Добавьте байк — карта откроется на его месте",
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(QaDimens.spaceM),
                )
                else -> Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(QaDimens.radiusButton))) {
                    BikeMap(
                        tileSource = tileSource,
                        tileLoader = tileLoader,
                        initialCenter = firstLocation,
                        focus = state.focus,
                        pins = state.rows.map { row ->
                            val task = (row.lookup as? BikeLookup.Found)?.bike?.status?.driverTask
                            BikePin(
                                id = row.bike.id.toString(),
                                label = row.bike.setup.simulatorId.toString(),
                                location = row.bike.setup.location,
                                color = chargeTargetColor(row.bike.setup.target),
                                isSelected = row.bike.id == state.selectedBikeId,
                                badgeColor = if (task == null) null else driverTaskColor(task),
                            )
                        },
                        zones = state.zones,
                        techZones = state.techZones,
                        onMapClick = {},
                        onPinMoved = { pinId, location -> onEvent(TemplateEvent.OnPinMoved(pinId, location)) },
                        onPinClick = { pinId -> onEvent(TemplateEvent.OnPinClicked(pinId)) },
                        onParkingAreaIdle = { center -> onEvent(TemplateEvent.OnParkingAreaIdle(center)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    PlaceSearchField(
                        cityName = state.cityName,
                        cities = state.cities,
                        techZones = state.techZones,
                        onCitySelected = { city -> onEvent(TemplateEvent.OnCitySelected(city)) },
                        onTechZoneSelected = { techZone -> onEvent(TemplateEvent.OnTechZoneSelected(techZone)) },
                        onPointSelected = { point -> onEvent(TemplateEvent.OnPointSelected(point)) },
                        modifier = Modifier.align(Alignment.TopStart).padding(QaDimens.spaceM).width(SearchFieldWidth),
                    )
                    MapLegend(modifier = Modifier.align(Alignment.BottomStart).padding(QaDimens.spaceM))
                }
            }
            val selectedRow = state.rows.firstOrNull { it.bike.id == state.selectedBikeId }
            Text(
                text = if (selectedRow == null) {
                    "Нажмите на байк в списке или его пин, чтобы изменить заряд, доступ или место"
                } else {
                    "S.${selectedRow.bike.setup.simulatorId}: перетащите пин или выберите техзону слева"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedRow == null) QaColors.textSecondary else QaColors.brandText,
                modifier = Modifier.padding(horizontal = QaDimens.spaceS),
            )
            if (state.isZonesFailed) {
                Text("Не удалось загрузить парковки и зоны", color = QaColors.negative, modifier = Modifier.padding(horizontal = QaDimens.spaceS))
            }
        }
    }
}
