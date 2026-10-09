package ui.chat.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags
import ui.chat.model.ChatAttachmentUi
import ui.chat.model.ReasoningLevelUi
import ui.common.MessageTextField
import ui.theme.DroidMentorUiTokens

/**
 * Chat-specific composer kept independent from ActiveChatUiState and backend effects.
 * Known controls are explicit inputs so later provider/file integrations do not redesign it.
 */
@Composable
fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    canSend: Boolean,
    isGenerating: Boolean,
    attachmentsEnabled: Boolean,
    attachment: ChatAttachmentUi?,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    supportedReasoningLevels: Set<ReasoningLevelUi>,
    reasoningLevel: ReasoningLevelUi,
    onReasoningSelected: (ReasoningLevelUi) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composerShape = RoundedCornerShape(DroidMentorUiTokens.ComposerRadius)

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = DroidMentorUiTokens.ComposerShadow,
                    shape = composerShape,
                    clip = false,
                )
                .background(MaterialTheme.colorScheme.surfaceVariant, composerShape)
                .border(
                    width = DroidMentorUiTokens.ComposerBorder,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = composerShape,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            if (attachment != null) {
                AttachmentDraft(
                    attachment = attachment,
                    onRemove = onRemoveAttachment,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            MessageTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag(ActiveChatTestTags.COMPOSER_FIELD),
                placeholder = "Message DroidMentor…",
            )

            ComposerActions(
                isGenerating = isGenerating,
                canSend = canSend,
                attachmentsEnabled = attachmentsEnabled,
                supportedReasoningLevels = supportedReasoningLevels,
                reasoningLevel = reasoningLevel,
                onAttachClick = onAttachClick,
                onReasoningSelected = onReasoningSelected,
                onSend = onSend,
                onStop = onStop,
            )
        }
    }
}
