
package ui.historyChat

import androidx.compose.runtime.Composable
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import ui.historyChat.components.ConversationGroup
import ui.navigation.NavigationAction

@Composable
fun HistoryNavigation(
    onNavigate: (NavigationAction) -> Unit,
    repository: ConversationRepository,
    currentConversationId: ConversationId? = null,
    onDeleteConversation: (ConversationId) -> Unit = {}
) {
    val groups = listOf(
        ConversationGroup(
            title = "Conversations",
            conversations = repository.conversations.map { conversation ->
                ConversationSummary(
                    id = conversation.id.value,
                    title = conversation.messages.firstOrNull()?.content?.take(40) ?: "New conversation",
                    lastActivity = if (repository.activeId == conversation.id) "Active" else "Saved"
                )
            }
        )
    )

    ChatHistoryScreen(
        groups = groups,
        isLoading = false,
        errorMessage = null,

        onAction = { action ->
            when (action) {
                is ChatHistoryAction.OpenConversation -> {
                    onNavigate(
                        NavigationAction.OpenConversation(
                            ConversationId(action.conversationId)
                        )
                    )
                }

                ChatHistoryAction.NewConversation -> {
                    onNavigate(NavigationAction.NewChat)
                }

                is ChatHistoryAction.DeleteConversation -> {
                    onDeleteConversation(
                        ConversationId(action.conversationId)
                    )
                }
            }
        },

        onSearch = {},

        onSettings = {
            onNavigate(NavigationAction.OpenSettings)
        },

        onAbout = {
            onNavigate(NavigationAction.OpenAbout)
        },

        onTitleClick = {
            onNavigate(NavigationAction.OpenTitle)
        }
    )
}
