package ui.preview

import ui.chat.ActiveChatUiState
import ui.chat.model.ChatAttachmentUi
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatMessageUi
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState
import ui.chat.model.ReasoningLevelUi

/** Deterministic fake states shared by previews and useful during manual UI review. */
object PreviewFixtures {
    private val user = ChatMessageUi(
        id = "user-1",
        role = ChatMessageRole.USER,
        content = "Why should the UI state stay independent from the provider?",
    )

    private val model = ChatMessageUi(
        id = "model-1",
        role = ChatMessageRole.MODEL,
        content = """
            Keeping UI state independent gives the screen one stable contract.

            It lets previews reproduce the screen without a provider, makes actions deterministic in tests, and lets later Gemini or Room work connect behind the same state/action boundary.

            The model turn deliberately stays plain text in UI-002; richer code rendering can be added later inside ModelMessageContent without redesigning the page.
        """.trimIndent(),
        canEdit = false,
    )

    val empty = ActiveChatUiState()

    val populated = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user, model),
    )

    val generating = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user),
        requestState = ChatRequestState.Generating,
    )

    val error = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user),
        requestState = ChatRequestState.Error(ChatErrorKind.RATE_LIMITED),
    )

    val editing = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user, model),
        editState = MessageEditState.Editing(
            messageId = user.id,
            draft = "Why should UI state stay independent from providers?",
        ),
    )


    val confirmRewrite = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user, model),
        editState = MessageEditState.ConfirmRewrite(
            messageId = user.id,
            replacementText = "Why should UI state stay independent from providers?",
        ),
    )

    val stopped = ActiveChatUiState(
        title = "Compose architecture",
        messages = listOf(user),
        requestState = ChatRequestState.Stopped,
    )

    val attachment = ActiveChatUiState(
        attachmentDraft = ChatAttachmentUi("architecture-notes.pdf"),
        reasoningLevel = ReasoningLevelUi.MEDIUM,
        supportedReasoningLevels = setOf(
            ReasoningLevelUi.AUTO,
            ReasoningLevelUi.LOW,
            ReasoningLevelUi.MEDIUM,
            ReasoningLevelUi.HIGH,
        ),
    )

    val offline = ActiveChatUiState(
        messages = listOf(user, model),
        draft = "I can keep writing this while offline.",
        isOnline = false,
    )
}
