package ui.historyChat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import kotlinx.coroutines.flow.collect
import ui.navigation.NavigationAction

/** Compose host: adapts the current snapshot-backed repository to a testable ViewModel. */
@Composable
fun HistoryNavigation(
    onNavigate: (NavigationAction) -> Unit,
    repository: ConversationRepository,
    onConversationDeleted: (ConversationId) -> Unit = {},
) {
    val model: HistoryViewModel = viewModel(
        key = "droidmentor-history",
        factory = HistoryViewModelFactory(repository),
    )
    val state by model.uiState.collectAsStateWithLifecycle()

    // Transitional ARCH-001 adapter: InMemoryConversationRepository uses Compose snapshots.
    // Replace this with repository Flow collection once the shared boundary supports it.
    LaunchedEffect(repository, model) {
        snapshotFlow { repository.conversations.toList() to repository.activeId }
            .collect { model.refresh() }
    }

    val dispatch: (ChatHistoryAction) -> Unit = { action ->
        when (val effect = model.onAction(action)) {
            is HistoryEffect.Navigate -> onNavigate(effect.action)
            is HistoryEffect.ConversationDeleted -> onConversationDeleted(effect.id)
            null -> Unit
        }
    }

    ChatHistoryScreen(
        groups = state.groups,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        pendingDeletion = state.pendingDeletion,
        onAction = dispatch,
        onSearch = {}, // Search is not yet part of the local milestone.
        onSettings = { dispatch(ChatHistoryAction.OpenSettings) },
        onAbout = { dispatch(ChatHistoryAction.OpenAbout) },
        onTitleClick = { dispatch(ChatHistoryAction.OpenTitle) },
    )
}
