package ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

class SettingsThemeUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun switchUpdatesGlobalThemeAndRendersBothStates() {
        composeRule.setContent {
            var dark by remember { mutableStateOf(false) }
            DroidMentorTheme(darkTheme = dark) {
                SettingsScreen(
                    state = SettingsUiState(),
                    isDarkTheme = dark,
                    onAction = { if (it == SettingsAction.ToggleThemeClicked) dark = !dark },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(SettingsTestTags.THEME_SWITCH).assertIsOff().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_SWITCH).assertIsOn().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_SWITCH).assertIsOff()
    }
}
