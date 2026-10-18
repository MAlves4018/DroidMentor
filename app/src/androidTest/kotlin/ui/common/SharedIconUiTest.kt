package ui.common

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

/** Vector assets must not change the accessible actions of existing UI primitives. */
class SharedIconUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun navigationIconsKeepTheirAccessibilityLabelsAndCallbacks() {
        var history = 0
        var newChat = 0
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                DroidMentorTopBar(
                    title = "DroidMentor",
                    onMenu = { history++ },
                    onNewChat = { newChat++ },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Open chat history").performClick()
        composeRule.onNodeWithContentDescription("New chat").performClick()
        composeRule.runOnIdle {
            assertEquals(1, history)
            assertEquals(1, newChat)
        }
    }

    @Test fun sendIconKeepsAccessibleButton() {
        var sends = 0
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                SendButton(onClick = { sends++ }, enabled = true)
            }
        }
        composeRule.onNodeWithContentDescription("Send message").performClick()
        composeRule.runOnIdle { assertEquals(1, sends) }
    }
}
