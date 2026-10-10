
package platform.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

import ui.title.TitleNavigation
import ui.chat.ActiveChatNavigation
import ui.settings.SettingsNavigation
import ui.about.AboutNavigation

@Composable
fun DroidMentorNavigation() {

    val navigator = rememberDroidMentorNavigator()
    val clipboardManager = LocalClipboardManager.current

    BackHandler(enabled = navigator.canGoBack) {
        navigator.back()
    }

    DroidMentorDrawer(navigator = navigator) {

        when (val destination = navigator.currentDestination) {

            DroidMentorDestination.Title -> {
                TitleNavigation(
                    onNavigate = navigator::handle
                )
            }

            is DroidMentorDestination.ActiveChat -> {
                ActiveChatNavigation(
                    conversationId = destination.conversationId,
                    onNavigate = navigator::handle,
                    onCopyText = { text ->
                        clipboardManager.setText(
                            AnnotatedString(text)
                        )
                    }
                )
            }

            DroidMentorDestination.Settings -> {
                SettingsNavigation(
                    onNavigate = navigator::handle
                )
            }

            DroidMentorDestination.About -> {
                AboutNavigation(
                    onNavigate = navigator::handle
                )
            }
        }
    }
}
