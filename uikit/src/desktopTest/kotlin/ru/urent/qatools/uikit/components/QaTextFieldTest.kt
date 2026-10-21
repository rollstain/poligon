package ru.urent.qatools.uikit.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QaTextFieldTest {

    @Test
    fun selectedPasswordOffersNeitherCopyNorCut() = runComposeUiTest {
        showField(this, PasswordVisualTransformation(), RecordingClipboard())

        val field = onNode(hasSetTextAction())
        field.performTextInputSelection(TextRange(0, SAVED_PASSWORD.length))

        field.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))
    }

    @Test
    fun copyShortcutLeavesPasswordOutOfClipboard() = runComposeUiTest {
        val clipboard = RecordingClipboard()
        showField(this, PasswordVisualTransformation(), clipboard)

        selectAllAndCopy(this)

        assertFalse(clipboard.hasReceivedText, "Пароль попал в буфер обмена")
    }

    @Test
    fun copyShortcutCopiesOrdinaryText() = runComposeUiTest {
        val clipboard = RecordingClipboard()
        showField(this, VisualTransformation.None, clipboard)

        selectAllAndCopy(this)

        assertTrue(clipboard.hasReceivedText, "Обычный текст должен копироваться, иначе проверка пароля ничего не доказывает")
    }

    private fun showField(test: ComposeUiTest, visualTransformation: VisualTransformation, clipboard: RecordingClipboard) {
        test.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard, LocalClipboardManager provides clipboard) {
                QaTextField(value = SAVED_PASSWORD, onValueChange = {}, label = "Пароль", visualTransformation = visualTransformation)
            }
        }
    }

    private fun selectAllAndCopy(test: ComposeUiTest) {
        val field = test.onNode(hasSetTextAction())
        field.performClick()
        field.performKeyInput {
            withKeyDown(Key.CtrlLeft) {
                pressKey(Key.A)
                pressKey(Key.C)
            }
        }
        test.waitForIdle()
    }

    private class RecordingClipboard : Clipboard, ClipboardManager {
        var hasReceivedText = false
            private set

        override val nativeClipboard: NativeClipboard = java.awt.datatransfer.Clipboard("test")

        override suspend fun getClipEntry(): ClipEntry? = null

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            if (clipEntry != null) hasReceivedText = true
        }

        override fun getText(): AnnotatedString? = null

        override fun setText(annotatedString: AnnotatedString) {
            hasReceivedText = true
        }
    }

    private companion object {
        const val SAVED_PASSWORD = "saved-password"
    }
}
