
package ui.historyChat

import androidx.compose.runtime.Composable
import domain.model.identity.ConversationId
import ui.historyChat.components.ConversationGroup
import ui.navigation.NavigationAction

@Composable
fun HistoryNavigation(
    onNavigate: (NavigationAction) -> Unit,
    currentConversationId: ConversationId? = null,
    onDeleteConversation: (ConversationId) -> Unit = {}
) {
    val groups = currentConversationId?.let { id ->
        listOf(
            ConversationGroup(
                title = "Current",
                conversations = listOf(
                    ConversationSummary(
                        id = id.value,
                        title = "Current conversation",
                        lastActivity = "Active"
                    )
                )
            )
        )
    } ?: emptyList()

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
