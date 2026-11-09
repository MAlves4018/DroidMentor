package ui.chat

import androidx.compose.runtime.Composable
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
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ActiveChatScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onCopyText = onCopyText,
    )
}
