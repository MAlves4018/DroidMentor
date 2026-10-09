package ui.chat.components.feedback

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags

/** Lightweight generation indicator shown as the pending model turn. */
@Composable
fun ThinkingIndicator(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "thinking")

    Row(
        modifier = modifier.testTag(ActiveChatTestTags.THINKING_INDICATOR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Thinking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))

        repeat(3) { index ->
            val dotAlpha = transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520,
                        delayMillis = index * 130,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "thinking-dot-$index",
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha.value)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        CircleShape,
                    ),
            )

            if (index < 2) {
                Spacer(Modifier.width(5.dp))
            }
        }
    }
}
