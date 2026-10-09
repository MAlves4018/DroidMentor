package ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.common.DroidMentorTopBar
import ui.common.InlineNotice
import ui.common.NoticeKind
import ui.settings.components.ApiKeyField
import ui.settings.components.ProviderCapabilitySection

/** Pure Settings surface: state in, user intents out. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            DroidMentorTopBar(
                title = "Settings",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = "GEMINI API KEY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InlineNotice(
                title = if (state.apiKeyConfigured) {
                    "Gemini API key configured"
                } else {
                    "Gemini API key not configured"
                },
                message = if (state.apiKeyConfigured) {
                    "You can replace or remove the configured key."
                } else {
                    "Add your own key to use Gemini."
                },
                kind = if (state.apiKeyConfigured) {
                    NoticeKind.INFO
                } else {
                    NoticeKind.WARNING
                },
                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 16.dp,
                ),
            )

            ApiKeyField(
                value = state.apiKeyDraft,
                revealed = state.apiKeyRevealed,
                configured = state.apiKeyConfigured,
                onValueChange = {
                    onAction(SettingsAction.ApiKeyChanged(it))
                },
                onToggleReveal = {
                    onAction(SettingsAction.ToggleApiKeyVisibility)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "DroidMentor keeps the key masked unless you choose Show.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(
                modifier = Modifier.height(18.dp),
            )

            Button(
                onClick = {
                    onAction(SettingsAction.SaveKeyClicked)
                },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsTestTags.SAVE_KEY),
            ) {
                Text("Save")
            }

            if (state.apiKeyConfigured) {
                TextButton(
                    onClick = {
                        onAction(SettingsAction.RemoveKeyClicked)
                    },
                    modifier = Modifier.testTag(SettingsTestTags.REMOVE_KEY),
                ) {
                    Text(
                        text = "Remove API key",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(32.dp),
            )

            ProviderCapabilitySection()
        }
    }
}
