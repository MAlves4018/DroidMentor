package ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared screen layout with the DroidMentor header.
 *
 * Screens provide their own content, optional header callbacks,
 * and optional bottom content.
 */
@Composable
fun MentorScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onNewChat: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DroidMentorTopBar(
                title = title,
                onMenu = onMenu,
                onBack = onBack,
                onNewChat = onNewChat
            )
        },
        bottomBar = bottomBar,
        content = content
    )
}