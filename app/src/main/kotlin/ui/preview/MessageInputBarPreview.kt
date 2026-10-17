package ui.common.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.common.MessageInputBar
import ui.theme.DroidMentorTheme

@Preview(name = "Shared - Message Input", widthDp = 390, showBackground = true)
@Composable
fun MessageInputBarPreview() {
    DroidMentorTheme(darkTheme = false) {
        MessageInputBar(value = "Explain coroutines", onValueChange = {}, onSend = {})
    }
}
