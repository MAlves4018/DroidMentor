package ui.chat.components.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags
import ui.chat.model.ReasoningLevelUi

/** Visible reasoning affordance; disabled until the provider declares supported levels. */
@Composable
fun ReasoningSelector(
    selected: ReasoningLevelUi,
    supported: Set<ReasoningLevelUi>,
    onSelected: (ReasoningLevelUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            enabled = supported.isNotEmpty(),
            modifier = Modifier.testTag(ActiveChatTestTags.REASONING_SELECTOR),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (supported.isEmpty()) {
                        "Reasoning: Auto"
                    } else {
                        "Reasoning: ${selected.displayLabel()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
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
