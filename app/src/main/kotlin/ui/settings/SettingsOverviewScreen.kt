package ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Mockup SET-01/02: General / Theme, Gemini / API key, About. */
@Composable
fun SettingsOverviewScreen(
    isDarkTheme: Boolean,
    apiKeyConfigured: Boolean,
    isLoading: Boolean = false,
    onThemeToggle: () -> Unit,
    onOpenApiKey: () -> Unit,
    onOpenAbout: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Text("GENERAL", color = muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Theme", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Filled.LightMode,
                contentDescription = null, modifier = Modifier.size(17.dp), tint = muted)
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { checked -> if (checked != isDarkTheme) onThemeToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .testTag(SettingsTestTags.THEME_SWITCH)
                    .semantics { contentDescription = "Toggle light and dark theme" },
            )
            Icon(imageVector = Icons.Filled.DarkMode,
                contentDescription = null, modifier = Modifier.size(17.dp), tint = muted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("GEMINI", modifier = Modifier.padding(top = 27.dp), color = muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth()
                .testTag(SettingsTestTags.API_KEY_ROW)
                .clickable(onClick = onOpenApiKey)
                .padding(vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("API key", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    isLoading -> "Checking… ›"
                    apiKeyConfigured -> "Configured  ›"
                    else -> "Not configured  ›"
                },
                color = muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("ABOUT", modifier = Modifier.padding(top = 27.dp), color = muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth()
                .testTag(SettingsTestTags.ABOUT_ROW)
                .clickable(onClick = onOpenAbout)
                .padding(vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("About DroidMentor", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("›", color = muted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
