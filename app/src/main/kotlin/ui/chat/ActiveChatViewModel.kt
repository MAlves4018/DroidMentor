package ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import domain.repository.ConversationRepository
import domain.repository.SettingsRepository
import domain.usecase.SendMessageUseCase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState
import kotlinx.coroutines.CoroutineStart

/** Owns one conversation's request lifecycle; Compose remains a pure state renderer. */
class ActiveChatViewModel(
    val conversationId: ConversationId,
    private val repository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    initialUiState: ActiveChatUiState = ActiveChatUiState(providerConfigured = false),
    newMessageId: () -> MessageId = { MessageId(UUID.randomUUID().toString()) },
) : ViewModel() {
    private val stateHolder = ActiveChatStateHolder(
        conversationId = conversationId,
        initialConversation = requireNotNull(repository.find(conversationId)) {
            "Cannot open unknown or deleted conversation: ${conversationId.value}"
        },
        initialUiState = initialUiState,
        onConversationChanged = repository::save,
        newMessageId = newMessageId,
    )

    val uiState: StateFlow<ActiveChatUiState> = stateHolder.uiState

    private var requestJob: Job? = null
    private var saveJob: Job? = null
    private var requestVersion = 0L
    private var pendingUserId: MessageId? = null

    init {
        viewModelScope.launch {
            settingsRepository.hasApiKey.collect { configured ->
                stateHolder.setProviderConfigured(configured)
            }
        }
    }

    fun onAction(action: ActiveChatAction) {
        when (action) {
            ActiveChatAction.SendClicked -> send()
            ActiveChatAction.RetryClicked -> retry()
            ActiveChatAction.StopClicked -> stop()

            ActiveChatAction.ConfirmRewriteClicked -> {
                viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    val before = stateHolder.currentConversation

                    try {
                        stateHolder.onAction(action)

                        if (before != stateHolder.currentConversation) {
                            pendingUserId = null
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Persistence failed; the original conversation remains.
                        stateHolder.setRequestState(
                            ChatRequestState.Error(ChatErrorKind.GENERIC_PROVIDER_ERROR)
                        )
                    }
                }
            }

            else -> {
                viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    stateHolder.onAction(action)
                }
            }
        }
    }

    private fun send() {
        if (
            saveJob?.isActive == true ||
            requestJob?.isActive == true ||
            !uiState.value.canSend
        ) return

        saveJob = viewModelScope.launch(
            start = CoroutineStart.UNDISPATCHED
        ) {
            try {
                val previousLastId =
                    stateHolder.currentConversation.messages.lastOrNull()?.id

                // Persist the USER message before requesting Gemini.
                stateHolder.onAction(ActiveChatAction.SendClicked)

                val last =
                    stateHolder.currentConversation.messages.lastOrNull()
                        ?: return@launch

                if (
                    last.id == previousLastId ||
                    last.role != Role.USER
                ) return@launch

                pendingUserId = last.id

                // Only runs after Room successfully saves the message.
                requestReply(last.id)

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The draft is preserved if persistence fails.
                stateHolder.setRequestState(
                    ChatRequestState.Error(ChatErrorKind.GENERIC_PROVIDER_ERROR)
                )
            }
        }
    }

    private fun retry() {
        if (
            saveJob?.isActive == true ||
            requestJob?.isActive == true
        ) return

        if (
            uiState.value.requestState !is ChatRequestState.Error &&
            uiState.value.requestState !is ChatRequestState.Stopped
        ) return

        val id = pendingUserId ?: return
        requestReply(id)
    }

    private fun stop() {
        val job = requestJob ?: return
        if (!job.isActive) return
        requestVersion++
        requestJob = null
        job.cancel()
        stateHolder.setRequestState(ChatRequestState.Stopped)
    }

    private fun requestReply(userId: MessageId) {
        if (requestJob?.isActive == true) return
        val history = stateHolder.currentConversation.messages.toList()
        if (history.lastOrNull()?.id != userId || history.lastOrNull()?.role != Role.USER) return
        if (repository.find(conversationId) == null) return
        val version = ++requestVersion
        stateHolder.setRequestState(ChatRequestState.Generating)
        requestJob = viewModelScope.launch {
            try {
                val reply = sendMessageUseCase(history)
                if (!currentCoroutineContext().isActive || version != requestVersion ||
                    repository.find(conversationId) == null ||
                    stateHolder.currentConversation.messages.lastOrNull()?.id != userId) return@launch
                if (stateHolder.appendModelReply(reply)) {
                    pendingUserId = null
                    stateHolder.setRequestState(ChatRequestState.Idle)
                } else {
                    stateHolder.setRequestState(ChatRequestState.Error(ChatErrorKind.PROTOCOL))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ProviderFailure) {
                if (version == requestVersion && repository.find(conversationId) != null) {
                    stateHolder.setRequestState(ChatRequestState.Error(failure.toChatErrorKind()))
                }
            } catch (_: Exception) {
                if (version == requestVersion && repository.find(conversationId) != null) {
                    stateHolder.setRequestState(ChatRequestState.Error(ChatErrorKind.GENERIC_PROVIDER_ERROR))
                }
            }
        }
    }

    private fun ProviderFailure.toChatErrorKind(): ChatErrorKind = when (this) {
        ProviderFailure.Offline -> ChatErrorKind.OFFLINE
        ProviderFailure.Authentication -> ChatErrorKind.INVALID_API_KEY
        is ProviderFailure.RateLimited -> ChatErrorKind.RATE_LIMITED
        is ProviderFailure.ServerError -> ChatErrorKind.SERVER_UNAVAILABLE
        ProviderFailure.Timeout -> ChatErrorKind.TIMEOUT
        is ProviderFailure.Transport -> ChatErrorKind.TRANSPORT
        is ProviderFailure.Protocol -> ChatErrorKind.PROTOCOL
        is ProviderFailure.ContentBlocked -> ChatErrorKind.CONTENT_BLOCKED
        is ProviderFailure.ResponseTruncated -> ChatErrorKind.RESPONSE_TRUNCATED
        is ProviderFailure.HttpError -> ChatErrorKind.GENERIC_PROVIDER_ERROR
    }
}
