package ui.chat.components.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.chat.model.ReasoningLevelUi

/** Lower composer row containing optional capabilities and the current primary action. */
@Composable
fun ComposerActions(
    isGenerating: Boolean,
    canSend: Boolean,
    attachmentsEnabled: Boolean,
    supportedReasoningLevels: Set<ReasoningLevelUi>,
    reasoningLevel: ReasoningLevelUi,
    onAttachClick: () -> Unit,
    onReasoningSelected: (ReasoningLevelUi) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (attachmentsEnabled) {
            AddAttachmentAction(
                enabled = !isGenerating,
                onClick = onAttachClick,
            )
        }

        ReasoningSelector(
            selected = reasoningLevel,
            supported = supportedReasoningLevels,
            onSelected = onReasoningSelected,
        )

        Spacer(modifier = Modifier.weight(1f))

        ComposerPrimaryAction(
            isGenerating = isGenerating,
            canSend = canSend,
            onSend = onSend,
            onStop = onStop,
        )
    }
}
