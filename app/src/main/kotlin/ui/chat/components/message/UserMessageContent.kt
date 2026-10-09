package ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import isel.dei.pdm.droidmentor.R
import ui.chat.ActiveChatTestTags
import ui.chat.model.ChatMessageUi
import ui.theme.DroidMentorUiTokens

/** Right-aligned user turn using the one-shot bubble geometry and contextual actions. */
@Composable
fun UserMessageContent(
    message: ChatMessageUi,
    canEdit: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleShape = RoundedCornerShape(
        topStart = DroidMentorUiTokens.MessageRadius,
        topEnd = DroidMentorUiTokens.MessageRadius,
        bottomStart = DroidMentorUiTokens.MessageRadius,
        bottomEnd = DroidMentorUiTokens.MessageTightCorner,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ActiveChatTestTags.userMessage(message.id)),
        horizontalAlignment = Alignment.End,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(MaterialTheme.colorScheme.surfaceVariant, bubbleShape)
                .padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            message.attachments.forEach { attachment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (message.content.isNotBlank()) 7.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_attach_file),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = attachment.displayName,
                        modifier = Modifier.padding(start = 7.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        MessageActionsMenu(
            messageId = message.id,
            canEdit = canEdit,
            onCopy = onCopy,
            onEdit = onEdit,
        )
    }
}
