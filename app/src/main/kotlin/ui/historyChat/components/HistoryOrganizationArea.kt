package ui.historyChat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Search action shown below New Chat.
 */
@Composable
fun HistoryOrganizationArea(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onSearch,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("⌕")
            Text(
                text = "Search chats",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}