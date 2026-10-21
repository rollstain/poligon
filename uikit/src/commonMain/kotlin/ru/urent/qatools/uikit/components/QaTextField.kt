package ru.urent.qatools.uikit.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

@Composable
fun QaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    placeholder: String? = null,
    maxLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = if (label == null) null else { { Text(label) } },
        placeholder = if (placeholder == null) null else { { Text(placeholder, color = QaColors.textSecondary) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorText != null,
        supportingText = if (errorText == null) null else { { Text(errorText) } },
        singleLine = maxLines == 1,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(QaDimens.radiusField),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = QaColors.background,
            unfocusedContainerColor = QaColors.background,
            disabledContainerColor = QaColors.background,
            errorContainerColor = QaColors.background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLabelColor = QaColors.textSecondary,
            unfocusedLabelColor = QaColors.textSecondary,
            cursorColor = QaColors.brand,
        ),
    )
}
