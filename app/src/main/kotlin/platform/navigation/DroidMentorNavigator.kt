package platform.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
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
    }

    fun back() {
        if (isHistoryOpen) {
            closeHistory()
        } else if (backStack.size > 1) {
            if (currentDestination is DroidMentorDestination.ActiveChat) repository.leaveActive()
            backStack.removeAt(backStack.lastIndex)
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
        val Saver = Saver<DroidMentorNavigator, ArrayList<String>>(
            save = { navigator ->
                ArrayList(
                    navigator.backStack.map { destination ->
                        when (destination) {
                            DroidMentorDestination.Title -> "title"
                            DroidMentorDestination.Settings -> "settings"
                            DroidMentorDestination.About -> "about"
                            is DroidMentorDestination.ActiveChat ->
                                "chat:${destination.conversationId.value}"
                        }
                    }
                )
            },
            restore = { saved ->
                val destinations = saved.mapNotNull { route ->
                    when {
                        route == "title" ->
                            DroidMentorDestination.Title

                        route == "settings" ->
                            DroidMentorDestination.Settings

                        route == "about" ->
                            DroidMentorDestination.About

                        route.startsWith("chat:") &&
                                route.length > 5 ->
                            DroidMentorDestination.ActiveChat(
                                ConversationId(route.removePrefix("chat:"))
                            )

                        else -> null
                    }
                }

                DroidMentorNavigator(
                    destinations.ifEmpty {
                        listOf(DroidMentorDestination.Title)
                    }
                )
            }
        )
    }
}

@Composable
fun rememberDroidMentorNavigator(repository: ConversationRepository): DroidMentorNavigator {
    return remember(repository) {
        val resume = repository.resumableId()
        DroidMentorNavigator(
            initialBackStack = listOf(DroidMentorDestination.Title) +
                    listOfNotNull(resume?.let { DroidMentorDestination.ActiveChat(it) }),
            repository = repository,
        )
    }
}