package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.DisabledReason

@OptIn(ExperimentalTestApi::class)
class AvailabilityFieldTest {

    @Test
    fun arrowDownAndEnterChooseNextMatchingReason() = runComposeUiTest {
        var chosen: AvailabilityTarget? = null
        setContent {
            AvailabilityField(target = AvailabilityTarget.Keep, reasons = Reasons, onTargetChange = { chosen = it }, label = FIELD_LABEL)
        }
        val field = onNode(hasSetTextAction())

        field.performClick()
        field.performTextInput("рем")
        field.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        assertEquals(AvailabilityTarget.Disabled(reasonName = "Repair", reasonTitle = REPAIR_TITLE), chosen)
        onNodeWithText(AvailabilityTargetKeepText).assertExists()
    }

    @Test
    fun escapeClosesSuggestionsWithoutChoosing() = runComposeUiTest {
        var chosen: AvailabilityTarget? = null
        setContent {
            AvailabilityField(target = AvailabilityTarget.Keep, reasons = Reasons, onTargetChange = { chosen = it }, label = FIELD_LABEL)
        }
        val field = onNode(hasSetTextAction())

        field.performClick()
        field.performTextInput("рем")
        field.performKeyInput { pressKey(Key.Escape) }

        assertNull(chosen)
        onNodeWithText(NEEDS_REPAIR_TITLE).assertDoesNotExist()
    }

    @Test
    fun clickOnButtonOutsideClosesSuggestions() = runComposeUiTest {
        var outsideClicks = 0
        setContent {
            Column {
                AvailabilityField(target = AvailabilityTarget.Keep, reasons = Reasons, onTargetChange = {}, label = FIELD_LABEL)
                Spacer(Modifier.height(OUTSIDE_BUTTON_OFFSET_DP.dp))
                Button(onClick = { outsideClicks++ }) { Text(OUTSIDE_BUTTON_TEXT) }
            }
        }
        val field = onNode(hasSetTextAction())

        field.performClick()
        field.performTextInput("рем")
        onNodeWithText(OUTSIDE_BUTTON_TEXT).performClick()

        assertEquals(1, outsideClicks)
        onNodeWithText(NEEDS_REPAIR_TITLE).assertDoesNotExist()
    }

    private companion object {
        const val OUTSIDE_BUTTON_TEXT = "Применить сейчас"
        const val OUTSIDE_BUTTON_OFFSET_DP = 400
        const val FIELD_LABEL = "Доступ"
        const val REPAIR_TITLE = "Ремонт"
        const val NEEDS_REPAIR_TITLE = "Требует ремонта"
        const val AvailabilityTargetKeepText = "Не менять доступ"
        val Reasons = listOf(
            DisabledReason(id = "1", name = "NeedsRepair", title = NEEDS_REPAIR_TITLE),
            DisabledReason(id = "2", name = "Repair", title = REPAIR_TITLE),
        )
    }
}
