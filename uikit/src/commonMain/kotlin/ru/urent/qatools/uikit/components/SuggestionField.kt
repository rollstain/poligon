package ru.urent.qatools.uikit.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

data class Suggestion<T>(val value: T, val title: String, val supportingText: String?)

data class SuggestionGroup<T>(val title: String?, val suggestions: List<Suggestion<T>>, val hiddenCount: Int)

private const val SELECTED_VALUE_MAX_LINES = 2
private val MenuMaxHeight = 440.dp

fun matchesQuery(text: String, query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.isEmpty() || text.contains(trimmed, ignoreCase = true)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SuggestionField(
    selectedText: String,
    label: String?,
    placeholder: String,
    groupsFor: (query: String) -> List<SuggestionGroup<T>>,
    onSelect: (T) -> Unit,
    suggestionIcon: @Composable (T) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    menuWidth: Dp? = null,
) {
    var isEditing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val groups = if (isEditing) groupsFor(query) else emptyList()
    val suggestions = groups.flatMap { it.suggestions }
    val activeIndex = if (suggestions.isEmpty()) -1 else highlightedIndex.coerceIn(0, suggestions.lastIndex)
    val requesters = remember(suggestions.size) { List(suggestions.size) { BringIntoViewRequester() } }
    val density = LocalDensity.current
    var fieldWidth by remember { mutableStateOf(0.dp) }

    fun finishEditing() {
        isEditing = false
        query = ""
        highlightedIndex = 0
        focusManager.clearFocus()
    }

    fun select(suggestion: Suggestion<T>) {
        onSelect(suggestion.value)
        finishEditing()
    }

    fun moveHighlight(step: Int) {
        if (suggestions.isEmpty()) return
        val nextIndex = (activeIndex + step).mod(suggestions.size)
        highlightedIndex = nextIndex
        val requester = requesters[nextIndex]
        scope.launch { requester.bringIntoView() }
    }

    Box(modifier = modifier) {
        QaTextField(
            value = if (isEditing) query else selectedText,
            onValueChange = { text ->
                query = text
                highlightedIndex = 0
                isEditing = true
            },
            label = label,
            placeholder = if (isEditing && selectedText.isNotEmpty()) selectedText else placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            maxLines = if (isEditing) 1 else SELECTED_VALUE_MAX_LINES,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> fieldWidth = with(density) { size.width.toDp() } }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        isEditing = true
                    } else if (isEditing) {
                        isEditing = false
                        query = ""
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            moveHighlight(1)
                            true
                        }
                        Key.DirectionUp -> {
                            moveHighlight(-1)
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            if (activeIndex >= 0) select(suggestions[activeIndex])
                            true
                        }
                        Key.Escape -> {
                            finishEditing()
                            true
                        }
                        else -> false
                    }
                },
        )
        DropdownMenu(
            expanded = isEditing,
            onDismissRequest = ::finishEditing,
            properties = PopupProperties(focusable = false),
            containerColor = QaColors.surface,
            modifier = Modifier
                .heightIn(max = MenuMaxHeight)
                .width(menuWidth ?: fieldWidth),
        ) {
            var index = 0
            for (group in groups) {
                if (group.suggestions.isEmpty() && group.hiddenCount == 0) continue
                val title = group.title
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                        modifier = Modifier.padding(start = QaDimens.spaceL, end = QaDimens.spaceL, top = QaDimens.spaceS),
                    )
                }
                for (suggestion in group.suggestions) {
                    val suggestionIndex = index
                    val isHighlighted = suggestionIndex == activeIndex
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(highlightMatch(suggestion.title, query), style = MaterialTheme.typography.bodyMedium)
                                val supportingText = suggestion.supportingText
                                if (supportingText != null) {
                                    Text(supportingText, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                                }
                            }
                        },
                        leadingIcon = { suggestionIcon(suggestion.value) },
                        trailingIcon = if (isHighlighted) {
                            { Text("Enter", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary) }
                        } else {
                            null
                        },
                        onClick = { select(suggestion) },
                        modifier = Modifier
                            .bringIntoViewRequester(requesters[suggestionIndex])
                            .background(if (isHighlighted) QaColors.brandSelection else Color.Transparent),
                    )
                    index++
                }
                if (group.hiddenCount > 0) {
                    Text(
                        text = "ещё ${group.hiddenCount} — уточните запрос",
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                        modifier = Modifier.padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceXs),
                    )
                }
            }
            if (suggestions.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(QaDimens.spaceL),
                )
            }
            HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
            Text(
                text = "↑ ↓ — выбрать · Enter — применить · Esc — закрыть",
                style = MaterialTheme.typography.bodySmall,
                color = QaColors.textSecondary,
                modifier = Modifier.padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceS),
            )
        }
    }
}

private fun highlightMatch(text: String, query: String): AnnotatedString {
    val trimmed = query.trim()
    val start = if (trimmed.isEmpty()) -1 else text.indexOf(trimmed, ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    val end = start + trimmed.length
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(start, end)) }
        append(text.substring(end))
    }
}
