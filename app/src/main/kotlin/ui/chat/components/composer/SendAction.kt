package ui.chat.components.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ui.chat.ActiveChatTestTags
import ui.common.SendButton

/** Chat semantic wrapper around the shared W1 send primitive. */
@Composable
fun SendAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SendButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.testTag(ActiveChatTestTags.SEND_ACTION),
    )
}
