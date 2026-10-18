package platform.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import domain.model.identity.ConversationId
import ui.navigation.NavigationAction
import domain.repository.ConversationRepository
import domain.repository.InMemoryConversationRepository

@Stable
class DroidMentorNavigator(
    initialBackStack: List<DroidMentorDestination> =
        listOf(DroidMentorDestination.Title),
    private val repository: ConversationRepository = InMemoryConversationRepository()
) {
    private val backStack =
        mutableStateListOf<DroidMentorDestination>().apply {
            addAll(initialBackStack)
        }

    var isHistoryOpen by mutableStateOf(false)
        private set

    val currentDestination: DroidMentorDestination
        get() = backStack.last()

    val canGoBack: Boolean
        get() = isHistoryOpen || backStack.size > 1

    val currentConversationId: ConversationId?
        get() = (currentDestination as?
                DroidMentorDestination.ActiveChat)?.conversationId

    private fun navigate(destination: DroidMentorDestination) {
        if (currentDestination != destination) {
            backStack.add(destination)
        }
    }

    fun openHistory() {
        isHistoryOpen = true
    }

    fun closeHistory() {
        isHistoryOpen = false
    }

    fun removeConversation(id: ConversationId) {
        backStack.removeAll { it is DroidMentorDestination.ActiveChat && it.conversationId == id }
        if (backStack.isEmpty()) backStack.add(DroidMentorDestination.Title)
        synchronizeActiveConversation()
    }

    /** Keep repository selection aligned with the surviving navigation stack. */
    private fun synchronizeActiveConversation() {
        val survivingChat = backStack.asReversed()
            .filterIsInstance<DroidMentorDestination.ActiveChat>()
            .firstOrNull { repository.find(it.conversationId) != null }
        if (survivingChat != null) {
            repository.activate(survivingChat.conversationId)
        } else {
            repository.leaveActive()
        }
    }

    fun back() {
        if (isHistoryOpen) {
            closeHistory()
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            synchronizeActiveConversation()
        }
    }

    fun handle(action: NavigationAction) {
        when (action) {
            NavigationAction.NewChat -> {
                closeHistory()
                val conversation = repository.create()
                navigate(DroidMentorDestination.ActiveChat(conversation.id))
            }

            NavigationAction.OpenTitle -> {
                closeHistory()
                repository.leaveActive()
                backStack.clear()
                backStack.add(DroidMentorDestination.Title)
            }

            NavigationAction.OpenHistory -> openHistory()

            is NavigationAction.OpenConversation -> {
                closeHistory()
                if (!repository.activate(action.conversationId)) return

                val destination =
                    DroidMentorDestination.ActiveChat(
                        action.conversationId
                    )

                val existingIndex =
                    backStack.indexOfLast { it == destination }

                if (existingIndex >= 0) {
                    while (backStack.lastIndex > existingIndex) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                } else {
                    navigate(destination)
                }
            }

            NavigationAction.OpenSettings -> {
                closeHistory()
                navigate(DroidMentorDestination.Settings)
            }

            NavigationAction.OpenAbout -> {
                closeHistory()
                navigate(DroidMentorDestination.About)
            }

            NavigationAction.Back -> back()
        }
    }

    companion object {
        /**
         * Restore routes against the existing application repository. A removed chat
         * cannot be restored as an active destination after process recreation.
         */
        fun saver(repository: ConversationRepository): Saver<DroidMentorNavigator, ArrayList<String>> =
            Saver(
                save = { navigator ->
                    ArrayList(navigator.backStack.map { destination ->
                        when (destination) {
                            DroidMentorDestination.Title -> "title"
                            DroidMentorDestination.Settings -> "settings"
                            DroidMentorDestination.About -> "about"
                            is DroidMentorDestination.ActiveChat ->
                                "chat:${destination.conversationId.value}"
                        }
                    })
                },
                restore = { routes ->
                    val destinations = routes.mapNotNull { route ->
                        when {
                            route == "title" -> DroidMentorDestination.Title
                            route == "settings" -> DroidMentorDestination.Settings
                            route == "about" -> DroidMentorDestination.About
                            route.startsWith("chat:") -> {
                                val value = route.removePrefix("chat:")
                                if (value.isBlank()) null else {
                                    val id = ConversationId(value)
                                    if (repository.find(id) != null) {
                                        DroidMentorDestination.ActiveChat(id)
                                    } else null
                                }
                            }
                            else -> null
                        }
                    }
                    DroidMentorNavigator(
                        initialBackStack = destinations.ifEmpty {
                            listOf(DroidMentorDestination.Title)
                        },
                        repository = repository,
                    ).also { it.synchronizeActiveConversation() }
                },
            )
    }
}

@Composable
fun rememberDroidMentorNavigator(repository: ConversationRepository): DroidMentorNavigator {
    return rememberSaveable(repository, saver = DroidMentorNavigator.saver(repository)) {
        val resume = repository.resumableId()
        DroidMentorNavigator(
            initialBackStack = listOf(DroidMentorDestination.Title) +
                    listOfNotNull(resume?.let { DroidMentorDestination.ActiveChat(it) }),
            repository = repository,
        )
    }
}