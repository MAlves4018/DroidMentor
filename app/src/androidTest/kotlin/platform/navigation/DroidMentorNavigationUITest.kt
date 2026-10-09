
package platform.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import ui.chat.ActiveChatTestTags
import ui.historyChat.HistoryTestTags
import ui.theme.DroidMentorTheme
import ui.title.TitleTestTags
import androidx.test.espresso.Espresso

class DroidMentorNavigationUiTest {

    @get:Rule
    val composeRule =
        createAndroidComposeRule<ComponentActivity>()

    private fun launchApp() {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                DroidMentorNavigation()
            }
        }
    }

    private fun openHistory() {
        composeRule
            .onNodeWithContentDescription("Open chat history")
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(HistoryTestTags.TITLE_BUTTON)
            .assertIsDisplayed()
    }

    private fun startNewChat() {
        launchApp()

        composeRule
            .onNodeWithContentDescription("New chat")
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT)
            .assertIsDisplayed()
    }

    private fun pressBack() {
        Espresso.pressBack()
        composeRule.waitForIdle()
    }

    @Test
    fun startsAtTitle() {
        launchApp()

        composeRule
            .onNodeWithText("How can I help you?")
            .assertIsDisplayed()
    }

    @Test
    fun titleOpensSettings() {
        launchApp()

        composeRule
            .onNodeWithTag(TitleTestTags.SETTINGS_BUTTON)
            .performClick()

        composeRule
            .onNodeWithText("GEMINI API KEY")
            .assertIsDisplayed()
    }

    @Test
    fun titleOpensAbout() {
        launchApp()

        composeRule
            .onNodeWithTag(TitleTestTags.ABOUT_BUTTON)
            .performClick()

        composeRule
            .onNodeWithText("WHAT IT IS")
            .assertIsDisplayed()
    }

    @Test
    fun settingsBackReturnsToTitle() {
        launchApp()

        composeRule
            .onNodeWithTag(TitleTestTags.SETTINGS_BUTTON)
            .performClick()

        pressBack()

        composeRule
            .onNodeWithText("How can I help you?")
            .assertIsDisplayed()
    }

    @Test
    fun aboutBackReturnsToTitle() {
        launchApp()

        composeRule
            .onNodeWithTag(TitleTestTags.ABOUT_BUTTON)
            .performClick()

        pressBack()

        composeRule
            .onNodeWithText("How can I help you?")
            .assertIsDisplayed()
    }

    @Test
    fun newChatOpensActiveChat() {
        startNewChat()
    }

    @Test
    fun titleCanOpenHistoryDrawer() {
        launchApp()
        openHistory()

        composeRule
            .onNodeWithTag(HistoryTestTags.NEW_CHAT_BUTTON)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(HistoryTestTags.SETTINGS_BUTTON)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(HistoryTestTags.ABOUT_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun historyDrawerCanStartNewChat() {
        launchApp()
        openHistory()

        composeRule
            .onNodeWithTag(HistoryTestTags.NEW_CHAT_BUTTON)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT)
            .assertIsDisplayed()
    }

    @Test
    fun historyDrawerOpensSettings() {
        launchApp()
        openHistory()

        composeRule
            .onNodeWithTag(HistoryTestTags.SETTINGS_BUTTON)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("GEMINI API KEY")
            .assertIsDisplayed()
    }

    @Test
    fun historyDrawerOpensAbout() {
        launchApp()
        openHistory()

        composeRule
            .onNodeWithTag(HistoryTestTags.ABOUT_BUTTON)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("WHAT IT IS")
            .assertIsDisplayed()
    }

    @Test
    fun historyTitleReturnsToTitleScreen() {
        startNewChat()
        openHistory()

        composeRule
            .onNodeWithTag(HistoryTestTags.TITLE_BUTTON)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("How can I help you?")
            .assertIsDisplayed()
    }

    @Test
    fun activeChatCanOpenHistory() {
        startNewChat()
        openHistory()

        composeRule
            .onNodeWithText("Current conversation")
            .assertIsDisplayed()
    }

    @Test
    fun selectingCurrentConversationReturnsToChat() {
        startNewChat()
        openHistory()

        composeRule
            .onNodeWithText("Current conversation")
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT)
            .assertIsDisplayed()
    }

    @Test
    fun backFromActiveChatReturnsToTitle() {
        startNewChat()

        pressBack()

        composeRule
            .onNodeWithText("How can I help you?")
            .assertIsDisplayed()
    }
}
