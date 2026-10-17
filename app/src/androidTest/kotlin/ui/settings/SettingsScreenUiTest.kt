package ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

import ui.theme.DroidMentorTheme

class SettingsScreenUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun overviewMatchesMockupSectionsAndNoReasoning() {
        setScreen(SettingsUiState(apiKeyConfigured = true))
        composeRule.onNodeWithText("GENERAL").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
        composeRule.onNodeWithText("GEMINI").assertExists()
        composeRule.onNodeWithText("API key").assertExists()
        composeRule.onNodeWithText("ABOUT").assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_SWITCH).assertExists()
        composeRule.onNodeWithText("Reasoning").assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.API_KEY_FIELD).assertDoesNotExist()
    }

    @Test fun themeSwitchEmitsTypedAction() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeRule.onNodeWithTag(SettingsTestTags.THEME_SWITCH).performClick()
        assertEquals(SettingsAction.ToggleThemeClicked, actions.last())
    }

    @Test fun apiKeyRowNavigatesToSubpage() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeRule.onNodeWithTag(SettingsTestTags.API_KEY_ROW).performClick()
        assertEquals(SettingsAction.OpenApiKeyClicked, actions.last())
    }

    @Test fun keySubpageOffersRevealAndDisabledSaveWhenEmpty() {
        setScreen(SettingsUiState(page = SettingsPage.API_KEY))
        composeRule.onNodeWithTag(SettingsTestTags.API_KEY_FIELD).assertExists()
        composeRule.onNodeWithText("Show").assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SAVE_KEY).assertIsNotEnabled()
        composeRule.onNodeWithTag(SettingsTestTags.REMOVE_KEY).assertDoesNotExist()
    }

    @Test fun apiKeyActionsAreTyped() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(page = SettingsPage.API_KEY), onAction = actions::add)
        composeRule.onNodeWithTag(SettingsTestTags.API_KEY_FIELD).performTextInput("abc")
        assertEquals(SettingsAction.ApiKeyChanged("abc"), actions.last())
        composeRule.onNodeWithText("Show").performClick()
        assertEquals(SettingsAction.ToggleApiKeyVisibility, actions.last())
    }

    @Test fun configuredRemoveRequestsConfirmationRatherThanImmediateDelete() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(page = SettingsPage.API_KEY, apiKeyConfigured = true), onAction = actions::add)
        composeRule.onNodeWithTag(SettingsTestTags.REMOVE_KEY).performClick()
        assertEquals(SettingsAction.RequestRemoveKeyClicked, actions.last())
    }

    private fun setScreen(
        state: SettingsUiState,
        onAction: (SettingsAction) -> Unit = {},
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                SettingsScreen(
                    state = state,
                    onAction = onAction,
                    onBack = {},
                )
            }
        }
    }
}
