package ui.settings.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ui.settings.SettingsTestTags

/** API-key input keeps the secret masked unless the user explicitly reveals it. */
@Composable
fun ApiKeyField(
    value: String,
    revealed: Boolean,
    configured: Boolean,
    onValueChange: (String) -> Unit,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.testTag(SettingsTestTags.API_KEY_FIELD),
        label = {
            Text("Gemini API key")
        },
        placeholder = {
            Text(
                if (configured) {
                    "Enter a replacement key"
                } else {
                    "Paste your API key"
                }
            )
        },
        trailingIcon = {
            TextButton(onClick = onToggleReveal) {
                Text(if (revealed) "Hide" else "Show")
            }
        },
        visualTransformation = if (revealed) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
        ),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )
}
