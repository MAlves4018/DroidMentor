package ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.settings.components.ApiKeyField

/** Mockup SET-03..07: secure key editor and explicit destructive confirmation. */
@Composable
fun SettingsApiKeyScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag(SettingsTestTags.API_KEY_PAGE),
    ) {
        Text("Gemini API key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ApiKeyField(
            value = state.apiKeyDraft,
            revealed = state.apiKeyRevealed,
            configured = state.apiKeyConfigured,
            onValueChange = { onAction(SettingsAction.ApiKeyChanged(it)) },
            onToggleReveal = { onAction(SettingsAction.ToggleApiKeyVisibility) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Text(
            "The key is stored locally on this device, outside automatic backups. Gemini validates it when used.",
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.isLoading) {
            Text("Reading saved settings…", style = MaterialTheme.typography.bodySmall)
        }
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = { onAction(SettingsAction.SaveKeyClicked) },
            enabled = state.canSave,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.SAVE_KEY),
        ) { Text(if (state.isSaving) "Saving…" else "Save") }
        if (state.apiKeyConfigured && !state.isLoading && !state.isSaving) {
            TextButton(
                onClick = { onAction(SettingsAction.RequestRemoveKeyClicked) },
                modifier = Modifier.padding(top = 9.dp).testTag(SettingsTestTags.REMOVE_KEY),
            ) { Text("Remove API key", color = MaterialTheme.colorScheme.error) }
        }
    }
}
