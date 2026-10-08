package ui.historyChat.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.historyChat.ChatHistoryAction
import ui.historyChat.ConversationSummary

@Composable
fun ConversationList(
    groups: List<ConversationGroup>,
    onAction: (ChatHistoryAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "CHATS",
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        groups.filter { it.conversations.isNotEmpty() }.forEach { group ->
            item(key = "header_${group.title}") {
                Text(
                    text = group.title.uppercase(),
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 12.dp,
                        bottom = 4.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = group.conversations,
                key = { it.id }
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onOpen = {
                        onAction(
                            ChatHistoryAction.OpenConversation(conversation.id)
                        )
                    },
                    onDelete = {
                        onAction(
                            ChatHistoryAction.DeleteConversation(conversation.id)
                        )
                    }
                )
            }
        }
    }
}

data class ConversationGroup(
    val title: String,
    val conversations: List<ConversationSummary>
)