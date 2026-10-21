package ru.urent.qatools.feature_qr_code.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import ru.urent.qatools.feature_qr_code.entities.ShownQrCode
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeEvent
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val EXAMPLE_NUMBER = "4072"
private const val CONTENT_TEXT_MAX_LINES = 2
private val InputFieldWidth = 300.dp
private val NumberPrefixFieldWidth = 260.dp
private val StripDividerHeight = 40.dp
private val HistoryMaxHeight = 112.dp
private val HistoryChipShape = RoundedCornerShape(14.dp)
private val HistoryChipTitleMaxWidth = 240.dp
private val SelectedChipBorderWidth = 2.dp
private val HistoryDeleteButtonSize = 32.dp
private val QrCodeGap = 24.dp
private val QrCodeCaptionHeight = 64.dp
private val QrCodeModuleColor = Color.Black
private val QrCodeBackgroundColor = Color.White

@Composable
fun QrCodeView(state: QrCodeState, onEvent: (QrCodeEvent) -> Unit) {
    val inputFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = QaDimens.screenPaddingHorizontal, vertical = QaDimens.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Header(isBackShown = state.isBackShown, onEvent = onEvent)
        InputStrip(state = state, inputFocus = inputFocus, onEvent = onEvent)
        HistoryStrip(state = state, onEvent = onEvent)
        QrCodesStage(modifier = Modifier.weight(1f).fillMaxWidth(), state = state)
    }
    LaunchedEffect(Unit) { inputFocus.requestFocus() }
}

@Composable
private fun Header(isBackShown: Boolean, onEvent: (QrCodeEvent) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
        Text("QR-код", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.alignByBaseline())
        Text(
            "Телефон считывает код прямо с экрана",
            style = MaterialTheme.typography.bodyMedium,
            color = QaColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).alignByBaseline(),
        )
        if (isBackShown) {
            Text(
                text = "← Вернуться к байку",
                style = MaterialTheme.typography.labelLarge,
                color = QaColors.brandText,
                modifier = Modifier
                    .alignByBaseline()
                    .clip(RoundedCornerShape(QaDimens.radiusField))
                    .clickable { onEvent(QrCodeEvent.OnBackClicked) }
                    .padding(horizontal = QaDimens.spaceS),
            )
        }
    }
}

@Composable
private fun InputStrip(state: QrCodeState, inputFocus: FocusRequester, onEvent: (QrCodeEvent) -> Unit) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
        ) {
            QaTextField(
                value = state.input,
                onValueChange = { input -> onEvent(QrCodeEvent.OnInputChanged(input)) },
                label = "Номер ТС или ссылка",
                placeholder = EXAMPLE_NUMBER,
                modifier = Modifier
                    .width(InputFieldWidth)
                    .focusRequester(inputFocus)
                    .onPreviewKeyEvent { event ->
                        val isEnter = event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)
                        if (isEnter) onEvent(QrCodeEvent.OnGenerateClicked)
                        isEnter
                    },
            )
            QaButton(text = "Показать", onClick = { onEvent(QrCodeEvent.OnGenerateClicked) }, enabled = state.pendingContent != null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                Text("В QR попадёт", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                val pendingContent = state.pendingContent
                val numberPrefix = state.numberPrefix
                val contentText = when {
                    state.isInputTooLong -> buildAnnotatedString { append("Слишком длинная строка — QR-код её не вместит") }
                    pendingContent == null -> buildAnnotatedString { append("Номер встанет в конец шаблона, ссылка или текст — как есть") }
                    pendingContent.startsWith(numberPrefix) -> buildAnnotatedString {
                        append(numberPrefix)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pendingContent.removePrefix(numberPrefix)) }
                    }
                    else -> buildAnnotatedString { append(pendingContent) }
                }
                Text(
                    contentText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        state.isInputTooLong -> QaColors.negative
                        pendingContent == null -> QaColors.textSecondary
                        else -> QaColors.textPrimary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            VerticalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke, modifier = Modifier.height(StripDividerHeight))
            var isNumberPrefixEditing by remember { mutableStateOf(false) }
            if (isNumberPrefixEditing) {
                QaTextField(
                    value = state.numberPrefix,
                    onValueChange = { numberPrefix -> onEvent(QrCodeEvent.OnNumberPrefixChanged(numberPrefix)) },
                    label = "Шаблон для номера",
                    modifier = Modifier.width(NumberPrefixFieldWidth),
                )
                TextButton(onClick = { isNumberPrefixEditing = false }) { Text("Готово") }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                    Text("Шаблон для номера", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                    Text(state.numberPrefix, style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = { isNumberPrefixEditing = true }) { Text("Изменить") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryStrip(state: QrCodeState, onEvent: (QrCodeEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
        Text(
            "История",
            style = MaterialTheme.typography.titleSmall,
            color = QaColors.textSecondary,
            modifier = Modifier.padding(top = QaDimens.spaceM),
        )
        if (state.isLoaded && state.history.isEmpty()) {
            Text(
                "Здесь появятся строки, для которых делали QR-код",
                style = MaterialTheme.typography.bodyMedium,
                color = QaColors.textSecondary,
                modifier = Modifier.padding(top = QaDimens.spaceM),
            )
        }
        FlowRow(
            modifier = Modifier.weight(1f).heightIn(max = HistoryMaxHeight).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
        ) {
            for (item in state.history) {
                val content = item.content
                Row(
                    modifier = Modifier
                        .clip(HistoryChipShape)
                        .background(if (item.isShown) QaColors.brandSelection else QaColors.surface)
                        .border(
                            if (item.isShown) BorderStroke(SelectedChipBorderWidth, QaColors.brand) else BorderStroke(QaDimens.dividerThickness, QaColors.stroke),
                            HistoryChipShape,
                        )
                        .padding(end = QaDimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.isShown,
                        onCheckedChange = { onEvent(QrCodeEvent.OnHistoryEntryCheckClicked(content)) },
                        colors = CheckboxDefaults.colors(checkedColor = QaColors.brand),
                        modifier = Modifier.semantics { contentDescription = "Показывать: $content" },
                    )
                    Text(
                        text = if (content.startsWith(state.numberPrefix)) content.removePrefix(state.numberPrefix) else content,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.isShown) QaColors.brandText else QaColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = HistoryChipTitleMaxWidth)
                            .clip(RoundedCornerShape(QaDimens.radiusField))
                            .clickable(onClickLabel = "Показать только этот код") { onEvent(QrCodeEvent.OnHistoryEntryClicked(content)) }
                            .padding(horizontal = QaDimens.spaceXs, vertical = QaDimens.spaceS),
                    )
                    Box(
                        modifier = Modifier
                            .size(HistoryDeleteButtonSize)
                            .clip(CircleShape)
                            .clickable { onEvent(QrCodeEvent.OnHistoryEntryDeleteClicked(content)) }
                            .semantics { contentDescription = "Убрать из истории: $content" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = QaColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodesStage(modifier: Modifier, state: QrCodeState) {
    Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(QaDimens.spaceXxl), contentAlignment = Alignment.Center) {
            val qrCodes = state.shownQrCodes
            if (!state.isLoaded) {
                Spacer(modifier = Modifier.fillMaxSize())
            } else if (qrCodes.isEmpty()) {
                Text("Введите номер или выберите строку из истории", color = QaColors.textSecondary)
            } else {
                var columnCount = 1
                var largestSide = 0.dp
                for (columns in 1..qrCodes.size) {
                    val rows = (qrCodes.size + columns - 1) / columns
                    val sideByWidth = (maxWidth - QrCodeGap * (columns - 1)) / columns
                    val sideByHeight = (maxHeight - QrCodeGap * (rows - 1)) / rows - QrCodeCaptionHeight
                    val side = minOf(sideByWidth, sideByHeight)
                    if (side <= largestSide) continue
                    largestSide = side
                    columnCount = columns
                }
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(QrCodeGap)) {
                    for (rowQrCodes in qrCodes.chunked(columnCount)) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QrCodeGap)) {
                            for (qrCode in rowQrCodes) {
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS, Alignment.CenterVertically),
                                ) {
                                    QrCodeImage(modifier = Modifier.weight(1f, fill = false), qrCode = qrCode)
                                    val content = qrCode.content
                                    if (content.startsWith(state.numberPrefix) && content.length > state.numberPrefix.length) {
                                        Text(content.removePrefix(state.numberPrefix), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        Text(content, style = MaterialTheme.typography.bodyMedium, color = QaColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Text(content, style = MaterialTheme.typography.bodyLarge, maxLines = CONTENT_TEXT_MAX_LINES, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            repeat(columnCount - rowQrCodes.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeImage(modifier: Modifier, qrCode: ShownQrCode) {
    val matrix = qrCode.matrix
    Canvas(modifier = modifier.aspectRatio(1f).semantics { contentDescription = "QR-код ${qrCode.content}" }) {
        val moduleSize = floor(size.minDimension / matrix.width)
        val origin = floor((size.minDimension - moduleSize * matrix.width) / 2)
        drawRect(QrCodeBackgroundColor)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        color = QrCodeModuleColor,
                        topLeft = Offset(origin + x * moduleSize, origin + y * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}
