package ui.chat.components.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags
import ui.chat.model.ReasoningLevelUi

/** Optional reasoning affordance; an empty supported set removes it from the composer. */
@Composable
fun ReasoningSelector(
    selected: ReasoningLevelUi,
    supported: Set<ReasoningLevelUi>,
    onSelected: (ReasoningLevelUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (supported.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(ActiveChatTestTags.REASONING_SELECTOR),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = "Reasoning: ${selected.displayLabel()} ▾",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            supported
                .sortedBy { it.ordinal }
                .forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.displayLabel()) },
                        onClick = {
                            expanded = false
                            onSelected(level)
                        },
                    )
                }
        }
    }
}

private fun ReasoningLevelUi.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.titlecase() }
