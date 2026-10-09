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

class SettingsScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unconfiguredState_isMaskedAndHasNoRemoveAction() {
        setScreen(
            state = SettingsUiState(
                apiKeyDraft = "preview-key-not-real",
            )
        )

        composeRule
            .onNodeWithText("Gemini API key not configured")
            .assertExists()

        composeRule
            .onNodeWithText("Show")
            .assertExists()

        composeRule
            .onNodeWithText("Remove API key")
            .assertDoesNotExist()

        composeRule
            .onNodeWithTag(SettingsTestTags.SAVE_KEY)
            .assertIsEnabled()
    }

    @Test
    fun revealedState_exposesHideControl() {
        setScreen(
            state = SettingsUiState(
                apiKeyDraft = "preview-key-not-real",
                apiKeyRevealed = true,
            )
        )

        composeRule
            .onNodeWithText("Hide")
            .assertExists()

        composeRule
            .onNodeWithText("Show")
            .assertDoesNotExist()
    }

    @Test
    fun fieldAndVisibility_emitActions() {
        val actions = mutableListOf<SettingsAction>()

        setScreen(
            state = SettingsUiState(),
            onAction = actions::add,
        )

        composeRule
            .onNodeWithTag(SettingsTestTags.API_KEY_FIELD)
            .performTextInput("abc")

        assertEquals(
            SettingsAction.ApiKeyChanged("abc"),
            actions.last(),
        )

        composeRule
            .onNodeWithText("Show")
            .performClick()

        assertEquals(
            SettingsAction.ToggleApiKeyVisibility,
            actions.last(),
        )
    }

    @Test
    fun emptyDraft_disablesSave() {
        setScreen(SettingsUiState())

        composeRule
            .onNodeWithTag(SettingsTestTags.SAVE_KEY)
            .assertIsNotEnabled()
    }

    @Test
    fun validDraft_saveEmitsAction() {
        val actions = mutableListOf<SettingsAction>()

        setScreen(
            state = SettingsUiState(
                apiKeyDraft = "abc",
            ),
            onAction = actions::add,
        )

        composeRule
            .onNodeWithTag(SettingsTestTags.SAVE_KEY)
            .assertIsEnabled()
            .performClick()

        assertEquals(
            SettingsAction.SaveKeyClicked,
            actions.last(),
        )
    }

    @Test
    fun configuredState_exposesRemove_andEmitsAction() {
        val actions = mutableListOf<SettingsAction>()

        setScreen(
            state = SettingsUiState(
                apiKeyConfigured = true,
            ),
            onAction = actions::add,
        )

        composeRule
            .onNodeWithText("Gemini API key configured")
            .assertExists()

        composeRule
            .onNodeWithTag(SettingsTestTags.REMOVE_KEY)
            .assertExists()
            .performClick()

        assertEquals(
            SettingsAction.RemoveKeyClicked,
            actions.last(),
        )
    }

    @Test
    fun exposesReservedReasoningCapabilityArea() {
        setScreen(SettingsUiState())

        composeRule
            .onNodeWithTag(SettingsTestTags.PROVIDER_CAPABILITIES)
            .assertExists()

        composeRule
            .onNodeWithText("Reasoning")
            .assertExists()
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
