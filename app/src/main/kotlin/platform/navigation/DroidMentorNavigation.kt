
package platform.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import isel.dei.pdm.droidmentor.DroidMentorApplication
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel

import ui.title.TitleNavigation
import ui.chat.ActiveChatNavigation
import ui.settings.SettingsNavigation
import ui.about.AboutNavigation

@Composable
fun DroidMentorNavigation(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {

    val repository = (LocalContext.current.applicationContext as DroidMentorApplication).conversations
    val navigator = rememberDroidMentorNavigator(repository)
    val chatScopes: ChatViewModelScopes = viewModel()
    val clipboardManager = LocalClipboardManager.current

    BackHandler(enabled = navigator.canGoBack) {
        navigator.back()
    }

    DroidMentorDrawer(
        navigator = navigator,
        repository = repository,
        onConversationDeleted = { id ->
            navigator.removeConversation(id)
            chatScopes.release(id)
        },
    ) {

        when (val destination = navigator.currentDestination) {

            DroidMentorDestination.Title -> {
                TitleNavigation(
                    onNavigate = navigator::handle
                )
            }

            is DroidMentorDestination.ActiveChat -> {
                ActiveChatNavigation(
                    conversationId = destination.conversationId,
                    repository = repository,
                    viewModelStoreOwner = chatScopes.ownerFor(destination.conversationId),
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
                    onNavigate = navigator::handle,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onToggleTheme,
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
