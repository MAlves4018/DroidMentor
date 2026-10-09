package ui.chat.components.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Chooses the mutually exclusive Send/Stop primary action from request state. */
@Composable
fun ComposerPrimaryAction(
    isGenerating: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isGenerating) {
        StopAction(
            onClick = onStop,
            modifier = modifier,
        )
    } else {
        SendAction(
            enabled = canSend,
            onClick = onSend,
            modifier = modifier,
        )
    }
}
