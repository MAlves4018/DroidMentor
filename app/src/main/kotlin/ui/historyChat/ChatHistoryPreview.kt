package ui.historyChat

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.historyChat.components.ConversationGroup
import ui.theme.DroidMentorTheme

/**
 * Sample conversation groups used only in previews.
 */
private val previewGroups = listOf(
    ConversationGroup(
        title = "Today",
        conversations = listOf(
            ConversationSummary(
                id = "1",
                title = "Jetpack Compose Navigation",
                lastActivity = "Today, 12:34"
            ),
            ConversationSummary(
                id = "2",
                title = "Understanding Kotlin Coroutines",
                lastActivity = "Today, 10:15"
            )
        )
    ),
    ConversationGroup(
        title = "Yesterday",
        conversations = listOf(
            ConversationSummary(
                id = "3",
                title = "Room Database Architecture",
                lastActivity = "Yesterday, 18:20"
            )
        )
    ),
    ConversationGroup(
        title = "Previous 7 Days",
        conversations = listOf(
            ConversationSummary(
                id = "4",
                title = "Ktor API Requests",
                lastActivity = "October 5, 21:10"
            )
        )
    ),
    ConversationGroup(
        title = "Older",
        conversations = emptyList()
    )
)

@Preview(
    name = "Chat History - Light",
    showBackground = true,
    heightDp = 800
)
@Composable
private fun ChatHistoryLightPreview() {
    DroidMentorTheme(darkTheme = false) {
        ChatHistoryScreen(
            groups = previewGroups,
            isLoading = false,
            errorMessage = null,
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            modifier = Modifier.width(316.dp)
        )
    }
}

@Preview(
    name = "Chat History - Dark",
    showBackground = true,
    heightDp = 800
)
@Composable
private fun ChatHistoryDarkPreview() {
    DroidMentorTheme(darkTheme = true) {
        ChatHistoryScreen(
            groups = previewGroups,
            isLoading = false,
            errorMessage = null,
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            modifier = Modifier.width(316.dp)
        )
    }
}

@Preview(
    name = "Chat History - Empty",
    showBackground = true,
    heightDp = 800
)
@Composable
private fun ChatHistoryEmptyPreview() {
    DroidMentorTheme(darkTheme = false) {
        ChatHistoryScreen(
            groups = emptyList(),
            isLoading = false,
            errorMessage = null,
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            modifier = Modifier.width(316.dp)
        )
    }
}

@Preview(
    name = "Chat History - Loading",
    showBackground = true,
    heightDp = 800
)
@Composable
private fun ChatHistoryLoadingPreview() {
    DroidMentorTheme(darkTheme = false) {
        ChatHistoryScreen(
            groups = emptyList(),
            isLoading = true,
            errorMessage = null,
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            modifier = Modifier.width(316.dp)
        )
    }
}

@Preview(
    name = "Chat History - Error",
    showBackground = true,
    heightDp = 800
)
@Composable
private fun ChatHistoryErrorPreview() {
    DroidMentorTheme(darkTheme = false) {
        ChatHistoryScreen(
            groups = emptyList(),
            isLoading = false,
            errorMessage = "Unable to load conversations.",
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            modifier = Modifier.width(316.dp)
        )
    }
}