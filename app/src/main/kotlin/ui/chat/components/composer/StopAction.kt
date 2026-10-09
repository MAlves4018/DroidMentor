package ui.chat.components.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import ui.chat.ActiveChatTestTags
import ui.theme.DroidMentorUiTokens
import ui.theme.StopRed

/** Destructive primary action shown only while a model response is generating. */
@Composable
fun StopAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(DroidMentorUiTokens.ComposerActionTouch)
            .testTag(ActiveChatTestTags.STOP_ACTION)
            .semantics {
                contentDescription = "Stop response"
                role = Role.Button
            }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DroidMentorUiTokens.ComposerActionVisual)
                .background(StopRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "■",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
