package ui.historyChat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import ui.historyChat.HistoryTestTags

/**
 * Footer displayed at the bottom of the Chat History drawer.
 *
 * Provides access to Settings and About without handling navigation.
 */
@Composable
fun HistoryDrawerFooter(
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalDivider()

        TextButton(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HistoryTestTags.SETTINGS_BUTTON)
        ) {
            Text(
                text = "Settings",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        TextButton(
            onClick = onAbout,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HistoryTestTags.ABOUT_BUTTON)
        ) {
            Text(
                text = "About",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}