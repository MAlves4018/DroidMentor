package presentation

import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import data.InMemorySettingsRepository
import test.MainDispatcherRule
import platform.navigation.DroidMentorDestination
import platform.navigation.DroidMentorNavigator
import test.fakes.FakeConversationRepository
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatViewModel
import ui.historyChat.ChatHistoryAction
import ui.historyChat.HistoryEffect
import ui.historyChat.HistoryViewModel
import ui.navigation.NavigationAction
import ui.settings.SettingsAction
import ui.settings.SettingsEffect
import ui.settings.SettingsViewModel
import ui.title.TitleAction
import ui.title.TitleEffect
import ui.title.TitleViewModel
import domain.model.Message
import domain.model.Role
import domain.provider.MentorProvider
import domain.usecase.SendMessageUseCase
import kotlinx.coroutines.runBlocking
import ui.chat.ActiveChatUiState

/** Contract tests across existing ViewModels + the same fake repository + router.
 * No Android, Gemini, Room, or implementation of future ARCH-001 interfaces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresentationIntegrationTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun chat(
        repo: FakeConversationRepository,
        id: ConversationId,
        prefix: String
    ): ActiveChatViewModel {
        var index = 0
        val settings = InMemorySettingsRepository()
        runBlocking { settings.saveApiKey("test-only-key") }
        val provider = object : MentorProvider {
            override suspend fun generateReply(
                history: List<Message>,
                apiKey: String
            ) = "Test mentor reply"
        }
        return ActiveChatViewModel(
            conversationId = id,
            repository = repo,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            initialUiState = ActiveChatUiState(providerConfigured = true),
            newMessageId = { MessageId("$prefix-${++index}") },
        )
    }

    @Test
    fun titleNewChat_createsRepositoryConversationThroughNavigator() = runTest {
        val repo = FakeConversationRepository()
        val navigator = DroidMentorNavigator(repository = repo)
        val effect = TitleViewModel().onAction(TitleAction.NewChatClicked)
        assertEquals(TitleEffect.Navigate(NavigationAction.NewChat), effect)
        navigator.handle((effect as TitleEffect.Navigate).action)
        assertEquals(1, repo.conversations.size)
        assertEquals(repo.activeId, navigator.currentConversationId)
    }

    @Test
    fun sendAndEdit_projectIntoHistoryWithoutSecondStore() = runTest {
        val repo = FakeConversationRepository()
        val id = repo.create().id
        val vm = chat(repo, id, "msg")
        vm.onAction(ActiveChatAction.DraftChanged("First question"))
        vm.onAction(ActiveChatAction.SendClicked)
        advanceUntilIdle()
        vm.onAction(ActiveChatAction.DraftChanged("Following question"))
        vm.onAction(ActiveChatAction.SendClicked)
        advanceUntilIdle()
        assertEquals(4, repo.find(id)!!.messages.size)
        assertEquals(2, repo.find(id)!!.messages.count { it.role == Role.USER })
        assertEquals(2, repo.find(id)!!.messages.count { it.role == Role.MODEL })

        val history = HistoryViewModel(repo)
        assertEquals(
            "First question",
            history.uiState.value.groups.single().conversations.single().title
        )
        val firstId = repo.find(id)!!.messages.first().id.value
        vm.onAction(ActiveChatAction.EditMessageClicked(firstId))
        vm.onAction(ActiveChatAction.EditDraftChanged("Revised first question"))
        vm.onAction(ActiveChatAction.SaveEditClicked)
        vm.onAction(ActiveChatAction.ConfirmRewriteClicked)
        history.refresh()
        assertEquals(
            listOf("Revised first question"),
            repo.find(id)!!.messages.map { it.content }
        )
        assertEquals(
            "Revised first question",
            history.uiState.value.groups.single().conversations.single().title
        )
    }

    @Test
    fun openingAFromHistory_keepsIndependentBData() = runTest {
        val repo = FakeConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        chat(repo, a, "A").apply {
            onAction(ActiveChatAction.DraftChanged("A question"))
            onAction(ActiveChatAction.SendClicked)
        }
        nav.handle(NavigationAction.NewChat)
        val b = nav.currentConversationId!!
        chat(repo, b, "B").apply {
            onAction(ActiveChatAction.DraftChanged("B question"))
            onAction(ActiveChatAction.SendClicked)
        }
        val effect = HistoryViewModel(repo).onAction(
            ChatHistoryAction.OpenConversation(a.value)
        )
        assertTrue(effect is HistoryEffect.Navigate)
        nav.handle((effect as HistoryEffect.Navigate).action)
        assertEquals(a, nav.currentConversationId)
        assertEquals("A question", repo.find(a)!!.messages.single().content)
        assertEquals("B question", repo.find(b)!!.messages.single().content)
        assertEquals(2, repo.conversations.size)
    }

    @Test
    fun deletingInactiveChat_preservesActiveChatAndEffects() = runTest {
        val repo = FakeConversationRepository()
        val a = repo.create().id
        val b = repo.create().id
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.OpenConversation(b))
        val history = HistoryViewModel(repo)
        history.onAction(ChatHistoryAction.RequestDeleteConversation(a.value))
        val effect = history.onAction(ChatHistoryAction.ConfirmDeleteConversation)
        assertEquals(HistoryEffect.ConversationDeleted(a), effect)
        nav.removeConversation(a)
        assertNull(repo.find(a))
        assertEquals(b, repo.activeId)
        assertEquals(b, nav.currentConversationId)
    }

    @Test
    fun deletingActiveChat_doesNotResurrectItOnReopen() = runTest {
        val repo = FakeConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        val history = HistoryViewModel(repo)
        history.onAction(ChatHistoryAction.RequestDeleteConversation(a.value))
        val effect = history.onAction(ChatHistoryAction.ConfirmDeleteConversation)
        assertEquals(HistoryEffect.ConversationDeleted(a), effect)
        nav.removeConversation(a)
        assertNull(repo.find(a))

        assertNull(repo.activeId)
        assertEquals(DroidMentorDestination.Title, nav.currentDestination)
        nav.handle(NavigationAction.OpenConversation(a))
        assertEquals(DroidMentorDestination.Title, nav.currentDestination)
    }

    @Test
    fun historyRejectsMissingId_withoutCreatingPhantomConversation() = runTest {
        val repo = FakeConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        val effect = HistoryViewModel(repo).onAction(
            ChatHistoryAction.OpenConversation("missing")
        )
        assertNull(effect)
        assertEquals(0, repo.conversations.size)
        nav.handle(
            NavigationAction.OpenConversation(ConversationId("missing"))
        )
        assertEquals(DroidMentorDestination.Title, nav.currentDestination)
    }

    @Test
    fun settingsDraftAndNavigation_doNotTouchConversations() = runTest {
        val conversations = FakeConversationRepository()
        val settingsRepository = InMemorySettingsRepository()
        val settings = SettingsViewModel(settingsRepository)
        advanceUntilIdle()
        settings.onAction(SettingsAction.ApiKeyChanged("dummy-api-key"))
        assertNull(settings.onAction(SettingsAction.SaveKeyClicked))
        advanceUntilIdle()
        assertEquals("dummy-api-key", settingsRepository.getApiKey())
        assertTrue(settings.uiState.value.apiKeyConfigured)
        assertTrue(conversations.conversations.isEmpty())
        settings.onAction(SettingsAction.RequestRemoveKeyClicked)
        settings.onAction(SettingsAction.RemoveKeyClicked)
        advanceUntilIdle()
        assertNull(settingsRepository.getApiKey())
        assertTrue(conversations.conversations.isEmpty())
    }

    @Test
    fun titleSendRemainsExplicitlyOutOfScope() = runTest {
        val repo = FakeConversationRepository()
        val title = TitleViewModel()
        title.onAction(TitleAction.DraftChanged("Not sent"))
        assertNull(title.onAction(TitleAction.SendClicked))
        assertEquals("Not sent", title.uiState.value.draft)
        assertTrue(repo.conversations.isEmpty())
    }
}
