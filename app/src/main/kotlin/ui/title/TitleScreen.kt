package ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import ui.chat.components.composer.ChatComposer
import ui.common.DroidMentorLogo
import ui.common.DroidMentorTopBar

/** The same composer geometry and reasoning placement as Chat (TIT-01..05). */
@Composable
fun TitleScreen(state: TitleUiState, onAction: (TitleAction) -> Unit) {
    Scaffold(
        topBar = {
            DroidMentorTopBar(
                title = "DroidMentor",
                onMenu = { onAction(TitleAction.OpenHistoryClicked) },
                                onNewChat = { onAction(TitleAction.NewChatClicked) },
            )
        },
        bottomBar = {
            ChatComposer(
                draft = state.draft,
                onDraftChange = { onAction(TitleAction.DraftChanged(it)) },
                canSend = false, // Title prompt handoff is not implemented; avoid a dead enabled action.
                isGenerating = false,
                attachmentsEnabled = true,
                attachment = null,
                onAttachClick = { onAction(TitleAction.AddAttachmentClicked) },
                onRemoveAttachment = {},
                supportedReasoningLevels = state.supportedReasoningLevels,
                reasoningLevel = state.reasoningLevel,
                onReasoningSelected = { onAction(TitleAction.ReasoningSelected(it)) },
                onSend = { onAction(TitleAction.SendClicked) },
                onStop = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DroidMentorLogo()
                Spacer(Modifier.height(22.dp))
                Text("DroidMentor", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("How can I help you?", modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onAction(TitleAction.OpenSettingsClicked) },
                        modifier = Modifier.testTag(TitleTestTags.SETTINGS_BUTTON)) { Text("Settings") }
                    TextButton(onClick = { onAction(TitleAction.OpenAboutClicked) },
                        modifier = Modifier.testTag(TitleTestTags.ABOUT_BUTTON)) { Text("About") }
                }
            }
        }
    }
}