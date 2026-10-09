package ui.chat.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ui.chat.model.ChatAttachmentUi
import ui.theme.DroidMentorUiTokens

/** Compact staged-attachment representation used before the real file pipeline exists. */
@Composable
fun AttachmentDraft(
    attachment: ChatAttachmentUi,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(13.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(start = 8.dp, top = 7.dp, bottom = 7.dp, end = 2.dp)
            .testTag(ActiveChatTestTags.ATTACHMENT_DRAFT),
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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(DroidMentorUiTokens.ComposerActionTouch)
                .testTag(ActiveChatTestTags.REMOVE_ATTACHMENT_ACTION),
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
