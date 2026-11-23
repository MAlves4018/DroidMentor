package ui.chat.components.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ui.chat.ActiveChatTestTags
import ui.chat.ActiveChatUiState
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState
import ui.common.InlineNotice
import ui.common.NoticeKind

/** Persistent availability and request feedback. One notice owns the active action. */
@Composable
fun RequestFeedback(
    state: ActiveChatUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckConnectivity: () -> Unit = {},
) {
    val feedbackModifier = modifier.testTag(ActiveChatTestTags.REQUEST_FEEDBACK)

    when {
        !state.isOnline -> InlineNotice(
            title = "No internet connection",
            message = "You can still read saved chats and edit a draft. Sending is disabled until Android validates the connection.",
            kind = NoticeKind.WARNING,
            actionLabel = "Check again",
            onAction = onCheckConnectivity,
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
                ChatErrorKind.OFFLINE -> {
                    title = "Connection lost during request"
                    message = "A previously submitted message remains saved. Retry after confirming internet access."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.INVALID_API_KEY -> {
                    title = "Gemini API key could not be used"
                    message = "Check or replace the API key in Settings."
                    actionLabel = "Open Settings"
                    action = onOpenSettings
                }
                ChatErrorKind.PERMISSION_DENIED -> {
                    title = "Access to Gemini denied"
                    message = "Check your project's access, permissions and API key in Settings or Google AI Studio."
                    actionLabel = "Open Settings"
                    action = onOpenSettings
                }
                ChatErrorKind.BILLING_REQUIRED -> {
                    title = "Gemini billing or account access required"
                    message = "The API reported a billing restriction. Review your project in Google AI Studio."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.MODEL_UNAVAILABLE -> {
                    title = "Gemini model unavailable"
                    message = "The configured model was not found or does not support this request."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.INVALID_REQUEST -> {
                    title = "Request rejected by Gemini"
                    message = "Gemini reported an invalid request. Repeating it unchanged may not help."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.QUOTA_EXCEEDED -> {
                    title = "Gemini quota reached"
                    message = "A quota restriction was reported. Check usage and limits before retrying."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.RATE_LIMITED -> {
                    title = "Too many requests"
                    message = "Gemini has temporarily limited requests. Try again later; your message remains saved."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.SERVER_UNAVAILABLE -> {
                    title = "Mentor temporarily unavailable"
                    message = "Gemini returned a server error. You can retry later."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.TIMEOUT -> {
                    title = "Request timed out"
                    message = "The request did not complete in time. Check connectivity before retrying."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.TRANSPORT -> {
                    title = "Could not reach Gemini"
                    message = "The device may have internet access but the Gemini service could not be reached."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.PROTOCOL -> {
                    title = "Unexpected Gemini response"
                    message = "The response could not be interpreted. You can retry."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.CONTENT_BLOCKED -> {
                    title = "Response blocked"
                    message = "The service blocked this reply. Consider rephrasing the request."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.RESPONSE_TRUNCATED -> {
                    title = "Response incomplete"
                    message = "The reply ended early. Retry or shorten the question."
                    actionLabel = "Retry"
                    action = onRetry
                }
                ChatErrorKind.LOCAL_STORAGE -> {
                    title = "Could not save conversation"
                    message = "A local storage operation failed. Do not assume the message was saved."
                    actionLabel = "Dismiss"
                    action = onDismiss
                }
                ChatErrorKind.GENERIC_PROVIDER_ERROR -> {
                    title = "Mentor request failed"
                    message = "The provider failed unexpectedly. Technical details can help diagnose this."
                    actionLabel = "Retry"
                    action = onRetry
                }
            }

            Column(modifier = feedbackModifier) {
                InlineNotice(
                    title = title,
                    message = message,
                    kind = NoticeKind.ERROR,
                    actionLabel = actionLabel,
                    onAction = action,
                )
                ChatErrorDetailsPanel(error, state.requestState.details)
            }
        }
    }
}
