package ui.chat.components.message

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatTestTags

/** Controlled edit surface so fake state, previews and W2 can reproduce the same edit state. */
@Composable
fun EditMessageView(
    draft: String,
    onDraftChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ActiveChatTestTags.EDIT_FIELD),
            minLines = 3,
        )

        Row(modifier = Modifier.padding(top = 6.dp)) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            TextButton(
                onClick = onSave,
                enabled = draft.isNotBlank(),
                modifier = Modifier.testTag(ActiveChatTestTags.EDIT_SAVE),
            ) {
                Text("Save")
            }
        }
    }
}
