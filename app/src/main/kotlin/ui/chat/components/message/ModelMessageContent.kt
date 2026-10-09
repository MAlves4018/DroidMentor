package ui.chat.components.message

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags
import ui.chat.model.ChatMessageUi

/** Open, left-aligned model turn. Rich code rendering can be added later inside this boundary. */
@Composable
fun ModelMessageContent(
    message: ChatMessageUi,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(ActiveChatTestTags.modelMessage(message.id)),
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
        )

        TextButton(onClick = { onCopyText(message.content) }) {
            Text(
                text = "Copy",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
