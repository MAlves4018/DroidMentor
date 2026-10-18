package ui.chat

import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.chat.model.ChatMessageRole
import ui.chat.model.MessageEditState

class ActiveChatStateHolderTest {

    @Test
    fun draftChanged_updatesUiStateWithoutChangingConversation() {
        val holder = holder()

        holder.onAction(ActiveChatAction.DraftChanged("Hello"))

        assertEquals("Hello", holder.state.draft)
        assertTrue(holder.currentConversation.messages.isEmpty())
    }

    @Test
    fun blankSend_isRejected() {
        val holder = holder()

        holder.onAction(ActiveChatAction.DraftChanged("   "))
        holder.onAction(ActiveChatAction.SendClicked)

        assertTrue(holder.currentConversation.messages.isEmpty())
        assertEquals("   ", holder.state.draft)
    }

    @Test
    fun validSend_insertsOneUserMessageWithStableIdentityAndClearsDraft() {
        val holder = holder(ids = listOf("u-generated"))

        holder.onAction(
            ActiveChatAction.DraftChanged("  Explain state hoisting  ")
        )
        holder.onAction(ActiveChatAction.SendClicked)

        val domainMessage = holder.currentConversation.messages.single()
        assertEquals("u-generated", domainMessage.id.value)
        assertEquals(Role.USER, domainMessage.role)
        assertEquals("Explain state hoisting", domainMessage.content)
        assertEquals(0L, domainMessage.sequence)

        val uiMessage = holder.state.messages.single()
        assertEquals("u-generated", uiMessage.id)
        assertEquals(ChatMessageRole.USER, uiMessage.role)
        assertEquals("", holder.state.draft)
    }

    @Test
    fun repeatedSend_doesNotDuplicateThePreviouslySubmittedDraft() {
        val holder = holder(ids = listOf("u1", "u2"))

        holder.onAction(ActiveChatAction.DraftChanged("Only once"))
        holder.onAction(ActiveChatAction.SendClicked)
        holder.onAction(ActiveChatAction.SendClicked)

        assertEquals(1, holder.currentConversation.messages.size)
        assertEquals("u1", holder.currentConversation.messages.single().id.value)
    }

    @Test
    fun unavailableChat_rejectsSendEvenWhenActionIsDispatchedDirectly() {
        val holder = ActiveChatStateHolder(
            conversationId = ConversationId("c1"),
            initialUiState = ActiveChatUiState(
                draft = "Prepared message",
                isOnline = false,
            ),
            newMessageId = { MessageId("u1") },
        )

        holder.onAction(ActiveChatAction.SendClicked)

        assertTrue(holder.currentConversation.messages.isEmpty())
        assertEquals("Prepared message", holder.state.draft)
    }

    @Test
    fun editActionFlow_rewritesSelectedUserMessageAndDropsFollowingSuffix() {
        val initial = Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "First question", 0))
            .append(message("m1", Role.MODEL, "First answer", 1))
            .append(message("u2", Role.USER, "Second question", 2))
            .append(message("m2", Role.MODEL, "Second answer", 3))

        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
        )

        holder.onAction(ActiveChatAction.EditMessageClicked("u2"))
        assertEquals(
            MessageEditState.Editing("u2", "Second question"),
            holder.state.editState,
        )

        holder.onAction(ActiveChatAction.EditDraftChanged("Replacement"))
        holder.onAction(ActiveChatAction.SaveEditClicked)
        assertEquals(
            MessageEditState.ConfirmRewrite("u2", "Replacement"),
            holder.state.editState,
        )

        holder.onAction(ActiveChatAction.ConfirmRewriteClicked)

        assertEquals(
            listOf("u1", "m1", "u2"),
            holder.currentConversation.messages.map { it.id.value },
        )
        assertEquals("Replacement", holder.currentConversation.messages.last().content)
        assertEquals(MessageEditState.None, holder.state.editState)
        assertEquals(
            holder.currentConversation.messages.size,
            holder.state.messages.size,
        )
    }

    @Test
    fun staleOrModelEditAction_isIgnored() {
        val initial = Conversation(ConversationId("c1")).append(
            message("m1", Role.MODEL, "Model response", 0)
        )

        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
        )

        holder.onAction(ActiveChatAction.EditMessageClicked("missing"))
        holder.onAction(ActiveChatAction.EditMessageClicked("m1"))

        assertEquals(MessageEditState.None, holder.state.editState)
        assertEquals(initial, holder.currentConversation)
    }

    @Test
    fun sendDuringEdit_isRejected() {
        val conversation = Conversation(
            id = ConversationId("c1"),
        ).append(
            Message(
                id = MessageId("u1"),
                role = Role.USER,
                content = "Original",
                sequence = 0L,
            )
        )

        val holder = ActiveChatStateHolder(
            conversationId = conversation.id,
            initialConversation = conversation,
            initialUiState = ActiveChatUiState(
                draft = "Another question",
                editState = MessageEditState.Editing(
                    messageId = "u1",
                    draft = "Replacement",
                ),
            ),
        )

        holder.onAction(ActiveChatAction.SendClicked)

        assertEquals(conversation, holder.currentConversation)
        assertEquals("Another question", holder.state.draft)
    }


    @Test
    fun failedSaveOnSend_doesNotAdvanceConversationOrClearDraft_andCanRetry() {
        var fail = true
        val initial = Conversation(ConversationId("c1"))
        val persisted = mutableListOf<Conversation>()
        var generated = 0
        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            onConversationChanged = { updated ->
                if (fail) throw IllegalStateException("storage unavailable")
                persisted.add(updated)
            },
            newMessageId = { MessageId("u${++generated}") },
        )
        holder.onAction(ActiveChatAction.DraftChanged("Retain this text"))
        val previousState = holder.state
        val exception = runCatching { holder.onAction(ActiveChatAction.SendClicked) }.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals(initial, holder.currentConversation)
        assertEquals(previousState, holder.state)
        assertTrue(persisted.isEmpty())

        fail = false
        holder.onAction(ActiveChatAction.SendClicked)
        assertEquals(1, persisted.size)
        assertEquals(1, holder.currentConversation.messages.size)
        assertEquals("Retain this text", holder.currentConversation.messages.single().content)
        assertEquals("", holder.state.draft)
    }

    @Test
    fun failedSaveOnRewrite_preservesFullSuffixAndConfirmation_forRetry() {
        val initial = Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "Original", 0))
            .append(message("m1", Role.MODEL, "Answer", 1))
        var fail = true
        var persisted: Conversation = initial
        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            onConversationChanged = { updated ->
                if (fail) throw IllegalStateException("storage unavailable")
                persisted = updated
            },
        )
        holder.onAction(ActiveChatAction.EditMessageClicked("u1"))
        holder.onAction(ActiveChatAction.EditDraftChanged("Changed"))
        holder.onAction(ActiveChatAction.SaveEditClicked)
        val previousState = holder.state

        val exception = runCatching { holder.onAction(ActiveChatAction.ConfirmRewriteClicked) }.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals(initial, holder.currentConversation)
        assertEquals(initial, persisted)
        assertEquals(previousState, holder.state)

        fail = false
        holder.onAction(ActiveChatAction.ConfirmRewriteClicked)
        assertEquals(1, persisted.messages.size)
        assertEquals("Changed", persisted.messages.single().content)
        assertEquals(persisted, holder.currentConversation)
        assertEquals(MessageEditState.None, holder.state.editState)
    }

    @Test
    fun synchronousSaveCallback_cannotReenterSendAndCreateDuplicateMessage() {
        val initial = Conversation(ConversationId("c1"))
        var callbackCalls = 0
        lateinit var holder: ActiveChatStateHolder
        holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            onConversationChanged = {
                callbackCalls++
                // Simulate an observer that synchronously dispatches the same intent.
                if (callbackCalls == 1) holder.onAction(ActiveChatAction.SendClicked)
            },
            newMessageId = { MessageId("u${callbackCalls + 1}") },
        )
        holder.onAction(ActiveChatAction.DraftChanged("Send exactly once"))
        holder.onAction(ActiveChatAction.SendClicked)

        assertEquals(1, callbackCalls)
        assertEquals(listOf("u1"), holder.currentConversation.messages.map { it.id.value })
        assertEquals("", holder.state.draft)
    }

    @Test
    fun synchronousSaveCallback_cannotReenterRewriteAndCommitTwice() {
        val initial = Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "Original", 0))
            .append(message("m1", Role.MODEL, "Response", 1))
        var callbackCalls = 0
        lateinit var holder: ActiveChatStateHolder
        holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            onConversationChanged = {
                callbackCalls++
                if (callbackCalls == 1) holder.onAction(ActiveChatAction.ConfirmRewriteClicked)
            },
        )
        holder.onAction(ActiveChatAction.EditMessageClicked("u1"))
        holder.onAction(ActiveChatAction.EditDraftChanged("Edited"))
        holder.onAction(ActiveChatAction.SaveEditClicked)
        holder.onAction(ActiveChatAction.ConfirmRewriteClicked)

        assertEquals(1, callbackCalls)
        assertEquals(1, holder.currentConversation.messages.size)
        assertEquals("Edited", holder.currentConversation.messages.single().content)
        assertEquals(MessageEditState.None, holder.state.editState)
    }

    @Test
    fun failedSave_releasesReentrancyGuardSoLaterRetryWorks() {
        val initial = Conversation(ConversationId("c1"))
        var callbacks = 0
        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            onConversationChanged = {
                callbacks++
                if (callbacks == 1) throw IllegalStateException("temporary failure")
            },
            newMessageId = { MessageId("u1") },
        )
        holder.onAction(ActiveChatAction.DraftChanged("Retry this"))
        assertTrue(runCatching { holder.onAction(ActiveChatAction.SendClicked) }.isFailure)
        assertEquals("Retry this", holder.state.draft)
        holder.onAction(ActiveChatAction.SendClicked)
        assertEquals(2, callbacks)
        assertEquals(1, holder.currentConversation.messages.size)
    }

    @Test
    fun confirmRewrite_duringGenerationIsIgnoredEvenIfConfirmationWasAlreadyOpen() {
        val initial = Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "Original", 0))
            .append(message("m1", Role.MODEL, "Reply", 1))
        var callbacks = 0
        val holder = ActiveChatStateHolder(
            conversationId = initial.id,
            initialConversation = initial,
            initialUiState = ActiveChatUiState(
                editState = MessageEditState.ConfirmRewrite("u1", "New version"),
                requestState = ui.chat.model.ChatRequestState.Generating,
            ),
            onConversationChanged = { callbacks++ },
        )
        holder.onAction(ActiveChatAction.ConfirmRewriteClicked)
        assertEquals(0, callbacks)
        assertEquals(initial, holder.currentConversation)
    }

    private fun holder(
        ids: List<String> = listOf("generated"),
    ): ActiveChatStateHolder {
        val iterator = ids.iterator()

        return ActiveChatStateHolder(
            conversationId = ConversationId("c1"),
            newMessageId = {
                MessageId(iterator.next())
            },
        )
    }

    private fun message(
        id: String,
        role: Role,
        content: String,
        sequence: Long,
    ) = Message(
        id = MessageId(id),
        role = role,
        content = content,
        sequence = sequence,
    )
}
