package ui.title

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ui.navigation.NavigationAction

/** Host routes one-off commands. TitleScreen only renders state and emits actions. */
@Composable
fun TitleNavigation(onNavigate: (NavigationAction) -> Unit) {
    val model: TitleViewModel = viewModel()
    val state by model.uiState.collectAsStateWithLifecycle()
    TitleScreen(
        state = state,
        onAction = { action ->
            when (val effect = model.onAction(action)) {
                is TitleEffect.Navigate -> onNavigate(effect.action)
                null -> Unit
            }
        },
    )
}
