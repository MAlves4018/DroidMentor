package ui.chat.components.message

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ui.chat.ActiveChatTestTags

/** Contextual copy/edit actions for a user turn. Branch controls remain a W9 concern. */
@Composable
fun MessageActionsMenu(
    messageId: String,
    canEdit: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(ActiveChatTestTags.messageActions(messageId)),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "Message actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    expanded = false
                    onCopy()
                },
            )

            if (canEdit) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    modifier = Modifier.testTag(ActiveChatTestTags.editAction(messageId)),
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                )
            }

        }
    }
}
