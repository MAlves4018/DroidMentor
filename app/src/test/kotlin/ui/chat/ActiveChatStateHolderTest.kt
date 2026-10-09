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
