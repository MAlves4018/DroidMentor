package ui.chat

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ui.chat.model.ChatAttachmentUi
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatMessageUi
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState
import ui.chat.model.ReasoningLevelUi
import ui.theme.DroidMentorTheme

class ActiveChatScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_disablesSend() {
        setScreen(ActiveChatUiState())

        composeRule.onNodeWithTag(ActiveChatTestTags.EMPTY_CONTENT).assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertIsNotEnabled()
    }

    @Test
    fun messages_preserveOrder_andUseRoleSpecificSurfaces() {
        val user = ChatMessageUi(
            id = "u1",
            role = ChatMessageRole.USER,
            content = "First user turn",
        )
        val model = ChatMessageUi(
            id = "m1",
            role = ChatMessageRole.MODEL,
            content = "Second model turn",
            canEdit = false,
        )

        setScreen(ActiveChatUiState(messages = listOf(user, model)))

        val userNode = composeRule
            .onNodeWithTag(ActiveChatTestTags.userMessage(user.id))
            .assertExists()
            .fetchSemanticsNode()
        val modelNode = composeRule
            .onNodeWithTag(ActiveChatTestTags.modelMessage(model.id))
            .assertExists()
            .fetchSemanticsNode()

        assertTrue(userNode.boundsInRoot.top < modelNode.boundsInRoot.top)
    }

    @Test
    fun typing_emitsDraftChanged() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(ActiveChatUiState(), actions::add)

        composeRule
            .onNodeWithTag(ActiveChatTestTags.COMPOSER_FIELD)
            .performTextInput("Hello")

        assertEquals(
            ActiveChatAction.DraftChanged("Hello"),
            actions.last(),
        )
    }

    @Test
    fun validDraft_sendEmitsSendClicked() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(
            state = ActiveChatUiState(draft = "Hello"),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).performClick()

        assertEquals(ActiveChatAction.SendClicked, actions.last())
    }

    @Test
    fun generating_showsThinkingAndStop_andStopEmitsAction() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(
            state = ActiveChatUiState(
                messages = listOf(
                    ChatMessageUi("u1", ChatMessageRole.USER, "Explain state hoisting")
                ),
                requestState = ChatRequestState.Generating,
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.THINKING_INDICATOR).assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.STOP_ACTION).assertExists().performClick()
        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertDoesNotExist()

        assertEquals(ActiveChatAction.StopClicked, actions.last())
    }

    @Test
    fun eligibleUserMessage_exposesEditAction() {
        val actions = mutableListOf<ActiveChatAction>()
        val message = ChatMessageUi("u1", ChatMessageRole.USER, "Editable")
        setScreen(
            state = ActiveChatUiState(messages = listOf(message)),
            onAction = actions::add,
        )

        composeRule
            .onNodeWithTag(ActiveChatTestTags.messageActions(message.id))
            .performClick()
        composeRule
            .onNodeWithTag(ActiveChatTestTags.editAction(message.id))
            .assertExists()
            .performClick()

        assertEquals(ActiveChatAction.EditMessageClicked(message.id), actions.last())
    }

    @Test
    fun ineligibleUserMessage_hidesEditAction() {
        val message = ChatMessageUi(
            id = "u1",
            role = ChatMessageRole.USER,
            content = "Not editable",
            canEdit = false,
        )
        setScreen(ActiveChatUiState(messages = listOf(message)))

        composeRule
            .onNodeWithTag(ActiveChatTestTags.messageActions(message.id))
            .performClick()
        composeRule
            .onNodeWithTag(ActiveChatTestTags.editAction(message.id))
            .assertDoesNotExist()
    }

    @Test
    fun attachmentEntryPoint_emitsAddAttachmentClicked() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(ActiveChatUiState(), actions::add)

        composeRule.onNodeWithTag(ActiveChatTestTags.ATTACH_ACTION).performClick()

        assertEquals(ActiveChatAction.AddAttachmentClicked, actions.last())
    }


    @Test
    fun selectedAttachment_countsAsSendableContent() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(
            state = ActiveChatUiState(
                attachmentDraft = ChatAttachmentUi("architecture.pdf"),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.ATTACHMENT_DRAFT).assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertIsEnabled().performClick()

        assertEquals(ActiveChatAction.SendClicked, actions.last())
    }

    @Test
    fun missingProvider_disablesSend_andExposesSettingsPath() {
        var openedSettings = false
        setScreen(
            state = ActiveChatUiState(
                draft = "Prepared question",
                providerConfigured = false,
            ),
            onOpenSettings = { openedSettings = true },
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithText("Open Settings").performClick()

        composeRule.runOnIdle { assertTrue(openedSettings) }
    }


    @Test
    fun reasoningSelector_whenSupported_emitsSelection() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(
            state = ActiveChatUiState(
                reasoningLevel = ReasoningLevelUi.AUTO,
                supportedReasoningLevels = setOf(
                    ReasoningLevelUi.AUTO,
                    ReasoningLevelUi.HIGH,
                ),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.REASONING_SELECTOR).performClick()
        composeRule.onNodeWithText("High").performClick()

        assertEquals(
            ActiveChatAction.ReasoningSelected(ReasoningLevelUi.HIGH),
            actions.last(),
        )
    }

    @Test
    fun error_showsFeedback_andRetryEmitsAction() {
        val actions = mutableListOf<ActiveChatAction>()
        setScreen(
            state = ActiveChatUiState(
                messages = listOf(
                    ChatMessageUi("u1", ChatMessageRole.USER, "Try this")
                ),
                requestState = ChatRequestState.Error(ChatErrorKind.RATE_LIMITED),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.REQUEST_FEEDBACK).assertExists()
        composeRule.onNodeWithText("Request limit reached").assertExists()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(ActiveChatAction.RetryClicked, actions.last())
    }

    @Test
    fun editState_isControlled_andSaveEmitsAction() {
        val actions = mutableListOf<ActiveChatAction>()
        val message = ChatMessageUi("u1", ChatMessageRole.USER, "Original")
        setScreen(
            state = ActiveChatUiState(
                messages = listOf(message),
                editState = MessageEditState.Editing(
                    messageId = message.id,
                    draft = "Replacement",
                ),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.EDIT_FIELD).assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.EDIT_SAVE).performClick()

        assertEquals(ActiveChatAction.SaveEditClicked, actions.last())
    }

    @Test
    fun offline_keepsDraftEditable_butDisablesSend() {
        setScreen(
            ActiveChatUiState(
                draft = "Prepared offline",
                isOnline = false,
            )
        )

        composeRule.onNodeWithText("You're offline").assertExists()
        composeRule.onNodeWithTag(ActiveChatTestTags.SEND_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithTag(ActiveChatTestTags.COMPOSER_FIELD).assertIsEnabled()
    }

    @Test
    fun confirmRewrite_emitsConfirmationAction() {
        val actions = mutableListOf<ActiveChatAction>()
        val message = ChatMessageUi("u1", ChatMessageRole.USER, "Original")
        setScreen(
            state = ActiveChatUiState(
                messages = listOf(message),
                editState = MessageEditState.ConfirmRewrite(
                    messageId = message.id,
                    replacementText = "Replacement",
                ),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(ActiveChatTestTags.CONFIRM_REWRITE).assertExists()
        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(ActiveChatAction.ConfirmRewriteClicked, actions.last())
    }

    private fun setScreen(
        state: ActiveChatUiState,
        onAction: (ActiveChatAction) -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                ActiveChatScreen(
                    state = state,
                    onAction = onAction,
                    onOpenHistory = {},
                    onOpenSettings = onOpenSettings,
                    onCopyText = {},
                )
            }
        }
    }
}
