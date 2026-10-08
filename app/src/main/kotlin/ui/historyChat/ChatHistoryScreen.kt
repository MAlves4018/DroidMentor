package ui.historyChat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.common.MentorEmpty
import ui.common.MentorError
import ui.common.MentorLoading
import ui.historyChat.components.ConversationGroup
import ui.historyChat.components.ConversationList
import ui.historyChat.components.HistoryDrawerFooter
import ui.historyChat.components.HistoryDrawerHeader
import ui.historyChat.components.HistoryOrganizationArea

/**
 * Composes the Chat History drawer UI.
 *
 * Receives presentation data and emits user actions.
 * Does not manage navigation, lifecycle, or persistence.
 */
@Composable
fun ChatHistoryScreen(
    groups: List<ConversationGroup>,
    isLoading: Boolean,
    errorMessage: String?,
    onAction: (ChatHistoryAction) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HistoryDrawerHeader(
                onNewChat = {
                    onAction(ChatHistoryAction.NewConversation)
                }
            )

            HistoryOrganizationArea(
                onSearch = onSearch
            )

            // This area fills the available height, keeping the footer at the bottom.
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        MentorLoading(
                            message = "Loading conversations..."
                        )
                    }

                    errorMessage != null -> {
                        MentorError(
                            message = errorMessage
                        )
                    }

                    groups.all { it.conversations.isEmpty() } -> {
                        MentorEmpty(
                            message = "No conversations yet."
                        )
                    }

                    else -> {
                        ConversationList(
                            groups = groups,
                            onAction = onAction,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            HistoryDrawerFooter(
                onSettings = onSettings,
                onAbout = onAbout
            )
        }
    }
}