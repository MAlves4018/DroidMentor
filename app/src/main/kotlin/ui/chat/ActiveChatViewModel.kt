package ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.model.Conversation
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import domain.provider.GeminiApiErrorCategory
import platform.connectivity.ConnectivityObserver
import platform.connectivity.AlwaysOnlineConnectivityObserver
import domain.repository.ConversationRepository
import domain.repository.SettingsRepository
import domain.usecase.SendMessageUseCase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatErrorDetails
import ui.chat.model.ConnectivityNotice
import ui.chat.model.ChatRequestState

class ActiveChatViewModel(
    val conversationId: ConversationId,
    private val repository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val connectivityObserver: ConnectivityObserver = AlwaysOnlineConnectivityObserver,
    initialUiState: ActiveChatUiState = ActiveChatUiState(
        providerConfigured = false,
        attachmentsEnabled = false,
        isOnline = connectivityObserver.isOnline.value,
    ),
    newMessageId: () -> MessageId = {
        MessageId(UUID.randomUUID().toString())
    },
) : ViewModel() {

    private val stateHolder = ActiveChatStateHolder(
        conversationId = conversationId,
        initialConversation = requireNotNull(
            repository.find(conversationId)
        ) {
            "Cannot open unknown or deleted conversation: ${conversationId.value}"
        },
        initialUiState = initialUiState,
        onConversationChanged = ::persistConversation,
        onRewriteRequested = ::rewritePersistedMessage,
        newMessageId = newMessageId,
    )

    val uiState: StateFlow<ActiveChatUiState> = stateHolder.uiState

    private val mutableConnectivityNotices = MutableSharedFlow<ConnectivityNotice>(extraBufferCapacity = 8)
    val connectivityNotices = mutableConnectivityNotices.asSharedFlow()

    private var requestJob: Job? = null
    private var saveJob: Job? = null
    private var requestVersion = 0L
    private var pendingUserId: MessageId? = null

    private suspend fun persistConversation(updated: Conversation) {
        try {
            repository.save(updated)
        } catch (cancelled: CancellationException) {
            val accepted = withContext(NonCancellable) {
                repository.initialize()
                repository.find(conversationId) == updated
            }

            if (!accepted) throw cancelled
        }
    }

    private suspend fun rewritePersistedMessage(
        messageId: MessageId,
        replacementText: String
    ): Conversation {
        try {
            return repository.rewriteUserMessage(
                conversationId = conversationId,
                messageId = messageId,
                replacementText = replacementText
            )
        } catch (cancelled: CancellationException) {
            val expected = stateHolder.currentConversation
                .rewriteUserMessage(
                    messageId = messageId,
                    replacementText = replacementText
                )

            val persisted = withContext(NonCancellable) {
                repository.initialize()
                repository.find(conversationId)
            }

            if (persisted == expected) {
                return persisted
            }

            throw cancelled
        }
    }

    init {
        viewModelScope.launch {
            var receivedInitial = false
            connectivityObserver.isOnline.collect { online ->
                val previous = uiState.value.isOnline
                stateHolder.setIsOnline(online)
                if (receivedInitial && online != previous) {
                    mutableConnectivityNotices.tryEmit(ConnectivityNotice(online, manualCheck = false))
                }
                receivedInitial = true
            }
        }
        viewModelScope.launch {
            settingsRepository.hasApiKey.collect { configured ->
                stateHolder.setProviderConfigured(configured)
            }
        }
    }

    fun onAction(action: ActiveChatAction) {
        when (action) {
            ActiveChatAction.SendClicked -> send()

            ActiveChatAction.CheckConnectivityClicked -> checkConnectivity()

            ActiveChatAction.RetryClicked -> retry()

            ActiveChatAction.StopClicked -> stop()

            ActiveChatAction.ConfirmRewriteClicked -> {
                if (!connectivityObserver.refresh()) {
                    stateHolder.setIsOnline(false)
                    mutableConnectivityNotices.tryEmit(ConnectivityNotice(false, manualCheck = true))
                    return
                }
                viewModelScope.launch(
                    start = CoroutineStart.UNDISPATCHED
                ) {
                    val before = stateHolder.currentConversation

                    try {
                        // Rewrite is persisted and reloaded first.
                        stateHolder.onAction(action)

                        if (before != stateHolder.currentConversation) {
                            // Ensure a previously cancelled request has
                            // finished before generating the new response.
                            requestJob?.join()

                            val last = stateHolder.currentConversation
                                .messages.lastOrNull()

                            pendingUserId = null

                            if (last?.role == Role.USER) {
                                pendingUserId = last.id

                                // Generates the replacement MODEL reply
                                // using the persisted rewritten history.
                                requestReply(last.id)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        stateHolder.setRequestState(
                            ChatRequestState.Error(ChatErrorKind.LOCAL_STORAGE)
                        )
                    }
                }
            }

            else -> {
                viewModelScope.launch(
                    start = CoroutineStart.UNDISPATCHED
                ) {
                    stateHolder.onAction(action)
                }
            }
        }
    }

    private fun checkConnectivity() {
        val online = connectivityObserver.refresh()
        stateHolder.setIsOnline(online)
        mutableConnectivityNotices.tryEmit(ConnectivityNotice(online, manualCheck = true))
    }

    private fun send() {
        if (!connectivityObserver.refresh()) {
            stateHolder.setIsOnline(false)
            return
        }
        if (
            saveJob?.isActive == true ||
            requestJob?.isCompleted == false ||
            !uiState.value.canSend
        ) return

        saveJob = viewModelScope.launch(
            start = CoroutineStart.UNDISPATCHED
        ) {
            try {
                val previousLastId =
                    stateHolder.currentConversation.messages
                        .lastOrNull()?.id

                stateHolder.onAction(
                    ActiveChatAction.SendClicked
                )

                val last =
                    stateHolder.currentConversation.messages
                        .lastOrNull() ?: return@launch

                if (
                    last.id == previousLastId ||
                    last.role != Role.USER
                ) return@launch

                pendingUserId = last.id

                requestReply(last.id)

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                stateHolder.setRequestState(
                    ChatRequestState.Error(ChatErrorKind.LOCAL_STORAGE)
                )
            }
        }
    }

    private fun retry() {
        if (!connectivityObserver.refresh()) {
            stateHolder.setIsOnline(false)
            return
        }
        if (
            saveJob?.isActive == true ||
            requestJob?.isCompleted == false
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
        job.cancel()

        stateHolder.setRequestState(
            ChatRequestState.Stopped
        )
    }

    private fun discardObsoleteRequest() {
        val persisted = repository.find(conversationId)
        if (persisted != null) stateHolder.reconcileConversation(persisted)
        pendingUserId = persisted?.messages?.lastOrNull()
            ?.takeIf { it.role == Role.USER }?.id
        stateHolder.setRequestState(
            if (persisted != null && pendingUserId == null) ChatRequestState.Idle
            else ChatRequestState.Error(ChatErrorKind.PROTOCOL)
        )
    }

    private fun conversationIsCurrentAfterFailure(): Boolean = try {
        if (repository.find(conversationId)?.messages != stateHolder.currentConversation.messages) {
            discardObsoleteRequest()
            false
        } else {
            true
        }
    } catch (_: Exception) {
        stateHolder.setRequestState(ChatRequestState.Error(ChatErrorKind.LOCAL_STORAGE))
        false
    }

    private fun requestReply(userId: MessageId) {
        if (!connectivityObserver.isOnline.value) {
            stateHolder.setIsOnline(false)
            stateHolder.setRequestState(ChatRequestState.Error(ChatErrorKind.OFFLINE))
            return
        }
        if (
            !viewModelScope.isActive ||
            requestJob?.isCompleted == false
        ) return

        val version = ++requestVersion

        stateHolder.setRequestState(
            ChatRequestState.Generating
        )

        requestJob = viewModelScope.launch {
            var providerCallInProgress = false
            try {
                // Reload committed conversation history from Room.
                repository.initialize()

                if (
                    !currentCoroutineContext().isActive ||
                    version != requestVersion
                ) return@launch

                val history = repository.find(conversationId)
                    ?.messages
                    ?.toList()
                if (history == null) {
                    // The conversation disappeared while the request was starting.
                    // A current ViewModel must not remain stuck in Generating.
                    if (currentCoroutineContext().isActive && version == requestVersion) {
                        discardObsoleteRequest()
                    }
                    return@launch
                }

                if (
                    history.lastOrNull()?.id != userId ||
                    history.lastOrNull()?.role != Role.USER ||
                    stateHolder.currentConversation.messages != history
                ) {
                    discardObsoleteRequest()
                    return@launch
                }

                providerCallInProgress = true
                val reply = sendMessageUseCase(history)
                providerCallInProgress = false

                // Cancellation/version mismatch is owned by Stop or ViewModel teardown.
                // Do not publish a stale completion or overwrite its terminal state.
                if (
                    !currentCoroutineContext().isActive ||
                    version != requestVersion
                ) return@launch

                // The response belongs to an obsolete conversation snapshot.
                // Discard it and leave Generating deterministically.
                if (
                    repository.find(conversationId)?.messages != history ||
                    stateHolder.currentConversation.messages != history
                ) {
                    discardObsoleteRequest()
                    return@launch
                }

                if (stateHolder.appendModelReply(reply)) {
                    pendingUserId = null

                    stateHolder.setRequestState(
                        ChatRequestState.Idle
                    )
                } else {
                    stateHolder.setRequestState(
                        ChatRequestState.Error(
                            ChatErrorKind.PROTOCOL
                        )
                    )
                }

            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ProviderFailure) {
                if (version == requestVersion) {
                    if (!conversationIsCurrentAfterFailure()) return@launch
                    stateHolder.setRequestState(
                        ChatRequestState.Error(
                            failure.toChatErrorKind(),
                            failure.toChatErrorDetails()
                        )
                    )
                }
            } catch (_: Exception) {
                if (version == requestVersion) {
                    if (!conversationIsCurrentAfterFailure()) return@launch
                    stateHolder.setRequestState(
                        ChatRequestState.Error(
                            if (providerCallInProgress) ChatErrorKind.GENERIC_PROVIDER_ERROR
                            else ChatErrorKind.LOCAL_STORAGE
                        )
                    )
                }
            }
        }
    }

    private fun ProviderFailure.toChatErrorDetails(): ChatErrorDetails? = when (this) {
        is ProviderFailure.GeminiApiError -> ChatErrorDetails(httpStatus, apiStatus, reason)
        is ProviderFailure.HttpError -> ChatErrorDetails(httpStatus = statusCode)
        is ProviderFailure.ServerError -> ChatErrorDetails(httpStatus = statusCode)
        else -> null
    }

    private fun ProviderFailure.toChatErrorKind(): ChatErrorKind =
        when (this) {
            is ProviderFailure.GeminiApiError -> when (category) {
                GeminiApiErrorCategory.INVALID_API_KEY -> ChatErrorKind.INVALID_API_KEY
                GeminiApiErrorCategory.PERMISSION_DENIED -> ChatErrorKind.PERMISSION_DENIED
                GeminiApiErrorCategory.BILLING_REQUIRED -> ChatErrorKind.BILLING_REQUIRED
                GeminiApiErrorCategory.MODEL_UNAVAILABLE -> ChatErrorKind.MODEL_UNAVAILABLE
                GeminiApiErrorCategory.INVALID_REQUEST -> ChatErrorKind.INVALID_REQUEST
                GeminiApiErrorCategory.QUOTA_EXCEEDED -> ChatErrorKind.QUOTA_EXCEEDED
            }

            ProviderFailure.Offline ->
                ChatErrorKind.OFFLINE

            ProviderFailure.Authentication ->
                ChatErrorKind.INVALID_API_KEY

            is ProviderFailure.RateLimited ->
                ChatErrorKind.RATE_LIMITED

            is ProviderFailure.ServerError ->
                ChatErrorKind.SERVER_UNAVAILABLE

            ProviderFailure.Timeout ->
                ChatErrorKind.TIMEOUT

            is ProviderFailure.Transport ->
                ChatErrorKind.TRANSPORT

            is ProviderFailure.Protocol ->
                ChatErrorKind.PROTOCOL

            is ProviderFailure.ContentBlocked ->
                ChatErrorKind.CONTENT_BLOCKED

            is ProviderFailure.ResponseTruncated ->
                ChatErrorKind.RESPONSE_TRUNCATED

            is ProviderFailure.HttpError ->
                ChatErrorKind.GENERIC_PROVIDER_ERROR
        }
}
