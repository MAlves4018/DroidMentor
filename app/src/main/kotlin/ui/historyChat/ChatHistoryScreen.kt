package ui.historyChat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.common.MentorEmpty
import ui.common.MentorError
import ui.common.MentorLoading
import ui.historyChat.components.*

@Composable
fun ChatHistoryScreen(
    groups: List<ConversationGroup>,
    isLoading: Boolean,
    errorMessage: String?,
    onAction: (ChatHistoryAction) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            HistoryDrawerHeader(
                onNewChat = {
                    onAction(ChatHistoryAction.NewConversation)
                },
                onTitleClick = onTitleClick
            )

            HistoryOrganizationArea(
                onSearch = onSearch
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        MentorLoading(
                            message = "Loading conversations..."
                        )
                    }

                    errorMessage != null -> {
                        MentorError(message = errorMessage)
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
