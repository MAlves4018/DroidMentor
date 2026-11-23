package ui.chat

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ui.chat.model.ChatErrorDetails
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState
import ui.theme.DroidMentorTheme

/** UI contract only: no network, API key or live Gemini dependency. */
class ErrorConnectivityUiContractTest {
    @get:Rule val composeRule = createComposeRule()

    private fun display(
        state: ActiveChatUiState,
        actions: MutableList<ActiveChatAction> = mutableListOf(),
        onOpenSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                ActiveChatScreen(
                    state = state,
                    onAction = actions::add,
                    onOpenHistory = {},
                    onOpenSettings = onOpenSettings,
                    onCopyText = {},
                )
            }
        }
    }

    @Test
    fun offlineBannerBlocksSendKeepsDraftEditableAndChecksAgain() {
        val actions = mutableListOf<ActiveChatAction>()
        display(ActiveChatUiState(draft = "Keep this draft", isOnline = false), actions)

        composeRule.onNodeWithText("No internet connection").assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithTag(ActiveChatTestTags.COMPOSER_FIELD).assertIsEnabled()
        composeRule.onNodeWithText("Check again").performClick()
        assertEquals(ActiveChatAction.CheckConnectivityClicked, actions.last())
    }

    @Test
    fun invalidKeyShowsSettingsAndRedactedTechnicalMetadata() {
        var settingsOpened = false
        display(
            state = ActiveChatUiState(
                requestState = ChatRequestState.Error(
                    ChatErrorKind.INVALID_API_KEY,
                    ChatErrorDetails(400, "INVALID_ARGUMENT", "API_KEY_INVALID"),
                ),
            ),
            onOpenSettings = { settingsOpened = true },
        )

        composeRule.onNodeWithText("Gemini API key could not be used").assertExists()
        composeRule.onNodeWithText("Open Settings").performClick()
        composeRule.runOnIdle { assertTrue(settingsOpened) }
        composeRule.onNodeWithText("Technical details").performClick()
        composeRule.onNodeWithText("Error code: INVALID_API_KEY", substring = true).assertExists()
        composeRule.onNodeWithText("HTTP: 400", substring = true).assertExists()
        composeRule.onNodeWithText("Reason: API_KEY_INVALID", substring = true).assertExists()
        composeRule.onNodeWithText("API key not valid.", substring = true).assertDoesNotExist()
    }

    @Test
    fun rateLimitedShowsRetryAction() {
        val actions = mutableListOf<ActiveChatAction>()
        display(ActiveChatUiState(requestState = ChatRequestState.Error(ChatErrorKind.RATE_LIMITED)), actions)

        composeRule.onNodeWithText("Too many requests").assertExists()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(ActiveChatAction.RetryClicked, actions.last())
    }

    @Test
    fun localStorageFailureIsNotMisrepresentedAsGeminiFailure() {
        val actions = mutableListOf<ActiveChatAction>()
        display(ActiveChatUiState(requestState = ChatRequestState.Error(ChatErrorKind.LOCAL_STORAGE)), actions)

        composeRule.onNodeWithText("Could not save conversation").assertExists()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        composeRule.onNodeWithText("Dismiss").performClick()
        assertEquals(ActiveChatAction.DismissFeedbackClicked, actions.last())
    }

    @Test
    fun quotaErrorDoesNotOfferBlindRetry() {
        val actions = mutableListOf<ActiveChatAction>()
        display(ActiveChatUiState(requestState = ChatRequestState.Error(ChatErrorKind.QUOTA_EXCEEDED)), actions)

        composeRule.onNodeWithText("Gemini quota reached").assertExists()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        composeRule.onNodeWithText("Dismiss").performClick()
        assertEquals(ActiveChatAction.DismissFeedbackClicked, actions.last())
    }

    @Test
    fun unavailableModelShowsSpecificReasonRatherThanGenericFailure() {
        display(ActiveChatUiState(requestState = ChatRequestState.Error(ChatErrorKind.MODEL_UNAVAILABLE)))

        composeRule.onNodeWithText("Gemini model unavailable").assertExists()
        composeRule.onNodeWithText("Mentor request failed").assertDoesNotExist()
    }
}
