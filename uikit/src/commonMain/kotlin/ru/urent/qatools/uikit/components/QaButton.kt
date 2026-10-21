package ru.urent.qatools.uikit.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

enum class QaButtonStyle { Primary, Secondary }

@Composable
fun QaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: QaButtonStyle = QaButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val containerColor = when (style) {
        QaButtonStyle.Primary -> QaColors.brand
        QaButtonStyle.Secondary -> QaColors.brandSoft
    }
    val contentColor = when (style) {
        QaButtonStyle.Primary -> QaColors.textInvert
        QaButtonStyle.Secondary -> QaColors.brandText
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = QaDimens.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(QaDimens.radiusButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = QaColors.disabledBackground,
            disabledContentColor = QaColors.disabledContent,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = QaDimens.spaceXl),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
