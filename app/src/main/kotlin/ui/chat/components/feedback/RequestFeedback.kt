package ui.chat.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ui.chat.ActiveChatTestTags
import ui.chat.ActiveChatUiState
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState
import ui.common.InlineNotice
import ui.common.NoticeKind

/** Explicit availability/request feedback shown above the conversation surface. */
@Composable
fun RequestFeedback(
    state: ActiveChatUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedbackModifier = modifier.testTag(ActiveChatTestTags.REQUEST_FEEDBACK)

    when {
        !state.isOnline -> InlineNotice(
            title = "You're offline",
            message = "Saved chats are still available, but new mentor interactions are unavailable.",
            kind = NoticeKind.WARNING,
            modifier = feedbackModifier,
        )

        !state.providerConfigured -> InlineNotice(
            title = "Mentor provider not configured",
            message = "Configure your Gemini API key to start remote interactions.",
            actionLabel = "Open Settings",
            onAction = onOpenSettings,
            modifier = feedbackModifier,
        )

        state.requestState is ChatRequestState.Stopped -> InlineNotice(
            title = "Response stopped",
            message = "You can change the prompt or send another message.",
            kind = NoticeKind.STOPPED,
            actionLabel = "Dismiss",
            onAction = onDismiss,
            modifier = feedbackModifier,
        )

        state.requestState is ChatRequestState.Error -> {
            val error = state.requestState.kind
            val title: String
            val message: String
            val actionLabel: String
            val action: () -> Unit

            when (error) {
                ChatErrorKind.INVALID_API_KEY -> {
                    title = "API key couldn't be used"
                    message = "Check or replace the key in Settings."
                    actionLabel = "Open Settings"
                    action = onOpenSettings
                }

                ChatErrorKind.RATE_LIMITED -> {
                    title = "Request limit reached"
                    message = "Try again later. Your user message was preserved."
                    actionLabel = "Retry"
                    action = onRetry
                }

                ChatErrorKind.SERVER_UNAVAILABLE -> {
                    title = "Mentor temporarily unavailable"
                    message = "The provider returned a server error."
                    actionLabel = "Retry"
                    action = onRetry
                }

                ChatErrorKind.TIMEOUT -> {
                    title = "Request timed out"
                    message = "DroidMentor did not receive a response in time."
                    actionLabel = "Retry"
                    action = onRetry
                }

                ChatErrorKind.TRANSPORT -> {
                    title = "Request failed"
                    message = "DroidMentor couldn't reach the mentor service."
                    actionLabel = "Retry"
                    action = onRetry
                }

                ChatErrorKind.PROTOCOL -> {
                    title = "Unexpected response"
                    message = "The provider returned a response DroidMentor could not understand."
                    actionLabel = "Retry"
                    action = onRetry
                }

                ChatErrorKind.GENERIC_PROVIDER_ERROR -> {
                    title = "Mentor request failed"
                    message = "DroidMentor couldn't get a response."
                    actionLabel = "Retry"
                    action = onRetry
                }
            }

            InlineNotice(
                title = title,
                message = message,
                kind = NoticeKind.ERROR,
                actionLabel = actionLabel,
                onAction = action,
                modifier = feedbackModifier,
            )
        }
    }
}
