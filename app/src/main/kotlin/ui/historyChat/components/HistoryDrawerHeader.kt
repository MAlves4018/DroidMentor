package ui.historyChat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import ui.historyChat.HistoryTestTags

@Composable
fun HistoryDrawerHeader(
    onNewChat: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DroidMentor",
            modifier = Modifier
                .testTag(HistoryTestTags.TITLE_BUTTON)
                .clickable(onClick = onTitleClick),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HistoryTestTags.NEW_CHAT_BUTTON)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("+")
                Text(
                    text = "New chat",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
