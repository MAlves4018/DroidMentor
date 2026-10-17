package ui.common.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.common.DroidMentorTopBar
import ui.theme.DroidMentorTheme

@Preview(name = "Shared - Top Bar - Light", widthDp = 390, showBackground = true)
@Composable
fun DroidMentorTopBarPreview() {
    DroidMentorTheme(darkTheme = false) {
        DroidMentorTopBar(title = "DroidMentor", onMenu = {}, onNewChat = {})
    }
}
