package platform.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import isel.dei.pdm.droidmentor.DroidMentorApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ui.navigation.NavigationAction
import ui.title.TitleNavigation
import ui.chat.ActiveChatNavigation
import ui.settings.SettingsNavigation
import ui.about.AboutNavigation

@Composable
fun DroidMentorNavigation(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    val repository =
        (LocalContext.current.applicationContext as DroidMentorApplication)
            .conversations

    var initialized by remember(repository) { mutableStateOf(false) }
    var initializationFailed by remember(repository) { mutableStateOf(false) }

    // Load persisted conversations before creating the navigator.
    LaunchedEffect(repository) {
        try {
            repository.initialize()
            initialized = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            initializationFailed = true
        }
    }

    if (!initialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (initializationFailed) {
                Text("Unable to load conversations.")
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    val navigator = rememberDroidMentorNavigator(repository)

    // Saver restoration cannot call suspend functions.
    LaunchedEffect(navigator) {
        navigator.synchronizeActiveConversation()
    }
    val chatScopes: ChatViewModelScopes = viewModel()
    val clipboardManager = LocalClipboardManager.current

    val scope = rememberCoroutineScope()
    val navigationMutex = remember(navigator) { Mutex() }

    val onNavigate: (NavigationAction) -> Unit = { action ->
        scope.launch {
            navigationMutex.withLock {
                navigator.handle(action)
            }
        }
    }

    BackHandler(enabled = navigator.canGoBack) {
        onNavigate(NavigationAction.Back)
    }

    DroidMentorDrawer(
        navigator = navigator,
        repository = repository,
        onNavigate = onNavigate,
        onConversationDeleted = { id ->
            scope.launch {
                navigationMutex.withLock {
                    navigator.removeConversation(id)
                    chatScopes.release(id)
                }
            }
        },
    ) {
        when (val destination = navigator.currentDestination) {

            DroidMentorDestination.Title -> {
                TitleNavigation(
                    onNavigate = onNavigate
                )
            }

            is DroidMentorDestination.ActiveChat -> {
                ActiveChatNavigation(
                    conversationId = destination.conversationId,
                    repository = repository,
                    viewModelStoreOwner =
                        chatScopes.ownerFor(destination.conversationId),
                    onNavigate = onNavigate,
                    onCopyText = { text ->
                        clipboardManager.setText(
                            AnnotatedString(text)
                        )
                    }
                )
            }

            DroidMentorDestination.Settings -> {
                SettingsNavigation(
                    onNavigate = onNavigate,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onToggleTheme,
                )
            }

            DroidMentorDestination.About -> {
                AboutNavigation(
                    onNavigate = onNavigate
                )
            }
        }
    }
}