package ui.historyChat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.MentorConfirmationDialog
import ui.common.MentorIconButton
import ui.historyChat.ConversationSummary

/**
 * Displays a conversation row with an overflow menu.
 *
 * Requests confirmation before emitting the delete action.
 * Does not modify conversation data.
 */
@Composable
fun ConversationItem(
    conversation: ConversationSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = conversation.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        Box {
            MentorIconButton(
                onClick = {
                    menuExpanded = true
                }
            ) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Delete")
                    },
                    onClick = {
                        menuExpanded = false
                        showDeleteConfirmation = true
                    }
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        MentorConfirmationDialog(
            title = "Delete conversation?",
            message = "Are you sure you want to delete \"${conversation.title}\"?",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }
}