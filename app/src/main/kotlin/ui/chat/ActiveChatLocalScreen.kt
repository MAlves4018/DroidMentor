package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelStoreOwner
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import isel.dei.pdm.droidmentor.DroidMentorApplication

/** Compose host only: obtains dependencies, collects state and forwards typed actions. */
@Composable
fun ActiveChatLocalScreen(
    conversationId: ConversationId,
    repository: ConversationRepository,
    viewModelStoreOwner: ViewModelStoreOwner,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as DroidMentorApplication
    val viewModel: ActiveChatViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = ActiveChatViewModelFactory(
            conversationId = conversationId,
            repository = repository,
            provider = application.mentor,
            settings = application.settings,
            connectivityObserver = application.connectivityObserver,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.connectivityNotices.collect { notice ->
            val text = when {
                notice.online && notice.manualCheck -> "Internet connection available."
                notice.online -> "Internet connection restored."
                notice.manualCheck -> "No validated internet connection yet."
                else -> "Internet connection lost. Sending has been disabled."
            }
            snackbarHostState.showSnackbar(text, withDismissAction = true)
        }
    }

    ActiveChatScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onCopyText = onCopyText,
    )
}
