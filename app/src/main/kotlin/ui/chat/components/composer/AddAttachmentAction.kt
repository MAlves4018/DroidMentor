package ui.chat.components.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags
import ui.theme.DroidMentorUiTokens

/** Chat-specific paperclip entry point; file picking is connected outside W1. */
@Composable
fun AddAttachmentAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(DroidMentorUiTokens.ComposerActionTouch)
            .testTag(ActiveChatTestTags.ATTACH_ACTION),
    ) {
        Icon(
            imageVector = Icons.Filled.AttachFile,
            contentDescription = "Add file",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
