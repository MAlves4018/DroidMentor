package ui.chat

import data.InMemorySettingsRepository
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.GeminiApiErrorCategory
import domain.provider.ProviderFailure
import domain.usecase.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import test.MainDispatcherRule
import test.fakes.ControlledMentorProvider
import test.fakes.FakeConversationRepository
import ui.chat.model.ChatErrorDetails
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiErrorCategoryViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test
    fun allStructuredGeminiErrorCategoriesReachTheCorrectUiWithoutModelTurns() = runTest {
        val cases = listOf(
            GeminiApiErrorCategory.INVALID_API_KEY to ChatErrorKind.INVALID_API_KEY,
            GeminiApiErrorCategory.PERMISSION_DENIED to ChatErrorKind.PERMISSION_DENIED,
            GeminiApiErrorCategory.BILLING_REQUIRED to ChatErrorKind.BILLING_REQUIRED,
            GeminiApiErrorCategory.MODEL_UNAVAILABLE to ChatErrorKind.MODEL_UNAVAILABLE,
            GeminiApiErrorCategory.INVALID_REQUEST to ChatErrorKind.INVALID_REQUEST,
            GeminiApiErrorCategory.QUOTA_EXCEEDED to ChatErrorKind.QUOTA_EXCEEDED,
        )
        cases.forEachIndexed { index, (category, expectedKind) ->
            val repository = FakeConversationRepository()
            val conversation = repository.create()
            val settings = InMemorySettingsRepository().also { it.saveApiKey("test-only") }
            val provider = ControlledMentorProvider()
            val vm = ActiveChatViewModel(
                conversationId = conversation.id,
                repository = repository,
                settingsRepository = settings,
                sendMessageUseCase = SendMessageUseCase(provider, settings),
                newMessageId = { MessageId("error-user-$index") },
            )
            runCurrent()
            vm.onAction(ActiveChatAction.DraftChanged("Question"))
            vm.onAction(ActiveChatAction.SendClicked)
            runCurrent()
            assertEquals(1, provider.requests.size)

            val failure = ProviderFailure.GeminiApiError(category, 400, "INVALID_ARGUMENT", "TEST_REASON")
            provider.fail(0, failure)
            runCurrent()

            val state = vm.uiState.value.requestState as ChatRequestState.Error
            assertEquals(expectedKind, state.kind)
            assertEquals(ChatErrorDetails(400, "INVALID_ARGUMENT", "TEST_REASON"), state.details)
            assertEquals(listOf(Role.USER), repository.find(conversation.id)!!.messages.map { it.role })
        }
    }
}
