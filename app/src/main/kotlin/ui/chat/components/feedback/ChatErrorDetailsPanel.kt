package ui.chat.components.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.chat.model.ChatErrorDetails
import ui.chat.model.ChatErrorKind

/** Optional, redacted troubleshooting information; never prints Gemini's raw message. */
@Composable
fun ChatErrorDetailsPanel(
    kind: ChatErrorKind,
    details: ChatErrorDetails?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(kind) { mutableStateOf(false) }
    Column(modifier = modifier) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide technical details" else "Technical details")
        }
        if (expanded) {
            val diagnostic = buildList {
                add("Error code: ${kind.name}")
                details?.httpStatus?.let { add("HTTP: $it") }
                details?.apiStatus?.let { add("API status: $it") }
                details?.reason?.let { add("Reason: $it") }
            }.joinToString("  |  ")
            Text(
                text = diagnostic,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
