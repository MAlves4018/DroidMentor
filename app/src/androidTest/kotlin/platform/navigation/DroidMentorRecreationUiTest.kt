package platform.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import isel.dei.pdm.droidmentor.MainActivity
import org.junit.Rule
import org.junit.Test
import ui.chat.ActiveChatTestTags
import ui.historyChat.HistoryTestTags

/** Device gate: checks the real Activity lifecycle rather than invoking a Saver manually. */
class DroidMentorRecreationUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun rotationRecreatesSettingsAndBackReturnsToOriginalChat() {
        composeRule.onNodeWithContentDescription("New chat").performClick()
        composeRule.onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open chat history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HistoryTestTags.SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText("GEMINI API KEY").assertIsDisplayed()

        // Recreate the actual Activity, retaining the application repository.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("GEMINI API KEY").assertIsDisplayed()

        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT).assertIsDisplayed()
    }
}
