package platform.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import domain.model.identity.ConversationId
import ui.navigation.NavigationAction
import domain.repository.ConversationRepository

@Stable
class DroidMentorNavigator(
    initialBackStack: List<DroidMentorDestination> =
        listOf(DroidMentorDestination.Title),
    private val repository: ConversationRepository
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

    suspend fun removeConversation(id: ConversationId) {
        backStack.removeAll {
            it is DroidMentorDestination.ActiveChat &&
                it.conversationId == id
        }

        if (backStack.isEmpty()) {
            backStack.add(DroidMentorDestination.Title)
        }

        synchronizeActiveConversation()
    }

    /**
     * Keep repository selection aligned with the surviving navigation stack.
     *
     * Used for navigation actions during the current session, including
     * Back and conversation deletion.
     */
    suspend fun synchronizeActiveConversation() {
        val survivingChat = backStack.asReversed()
            .filterIsInstance<DroidMentorDestination.ActiveChat>()
            .firstOrNull {
                repository.find(it.conversationId) != null
            }

        if (survivingChat != null) {
            repository.activate(survivingChat.conversationId)
        } else {
            repository.leaveActive()
        }
    }

    suspend fun back() {
        if (isHistoryOpen) {
            closeHistory()
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            synchronizeActiveConversation()
        }
    }

    suspend fun handle(action: NavigationAction) {
        when (action) {
            NavigationAction.NewChat -> {
                closeHistory()

                val conversation = repository.create()

                navigate(
                    DroidMentorDestination.ActiveChat(
                        conversation.id
                    )
                )
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

                if (!repository.activate(action.conversationId)) {
                    return
                }

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
         * Restore navigation from Android saved state.
         *
         * Room is initialized before this Saver is used.
         *
         * The saved Android routes may be older than the active
         * conversation persisted in Room.
         *
         * Restoration therefore:
         * 1. Discards references to missing conversations.
         * 2. Prefers a valid active conversation from Room.
         * 3. Preserves the existing fallback when the saved
         *    foreground conversation was deleted.
         * 4. Preserves trailing Settings/About destinations.
         * 5. Provides Title as the safe navigation root.
         *
         * Normal Back and deletion behavior remains unchanged.
         */
        fun saver(
            repository: ConversationRepository
        ): Saver<DroidMentorNavigator, ArrayList<String>> =
            Saver(
                save = { navigator ->
                    ArrayList(
                        navigator.backStack.map { destination ->
                            when (destination) {
                                DroidMentorDestination.Title ->
                                    "title"

                                DroidMentorDestination.Settings ->
                                    "settings"

                                DroidMentorDestination.About ->
                                    "about"

                                is DroidMentorDestination.ActiveChat ->
                                    "chat:${destination.conversationId.value}"
                            }
                        }
                    )
                },

                restore = { routes ->

                    // Decode saved destinations, ignoring missing chats.
                    val destinations = routes.mapNotNull { route ->
                        when {
                            route == "title" ->
                                DroidMentorDestination.Title

                            route == "settings" ->
                                DroidMentorDestination.Settings

                            route == "about" ->
                                DroidMentorDestination.About

                            route.startsWith("chat:") -> {
                                val value = route.removePrefix("chat:")

                                if (value.isBlank()) {
                                    null
                                } else {
                                    val id = ConversationId(value)

                                    if (repository.find(id) != null) {
                                        DroidMentorDestination.ActiveChat(id)
                                    } else {
                                        null
                                    }
                                }
                            }

                            else -> null
                        }
                    }

                    // Read the valid active conversation from Room.
                    val persistedActive = repository.resumableId()

                    // Last surviving conversation in Android saved routes.
                    val lastSurvivingChat = destinations
                        .filterIsInstance<
                            DroidMentorDestination.ActiveChat
                        >()
                        .lastOrNull()
                        ?.conversationId

                    // Last conversation referenced by the original
                    // saved routes, even if it no longer exists.
                    val lastSavedChatIndex = routes.indexOfLast {
                        it.startsWith("chat:")
                    }

                    val lastSavedChat = routes
                        .getOrNull(lastSavedChatIndex)
                        ?.removePrefix("chat:")
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::ConversationId)

                    val savedForegroundWasRemoved =
                        lastSavedChat != null &&
                            repository.find(lastSavedChat) == null

                    // Do not infer a resume if the saved navigation
                    // explicitly returned to Title after that chat.
                    val returnedToTitleAfterSavedChat =
                        lastSavedChatIndex >= 0 &&
                            routes.drop(lastSavedChatIndex + 1)
                                .contains("title")

                    // Room takes priority when it has a valid active chat.
                    
                    // Preserve the existing navigation fallback only
                    // when the saved foreground chat was deleted and
                    // the saved stack did not subsequently return to Title.
                    val targetActive = persistedActive ?:
                        if (
                            savedForegroundWasRemoved &&
                            !returnedToTitleAfterSavedChat
                        ) {
                            lastSurvivingChat
                        } else {
                            null
                        }

                    // Keep the saved stack when its last surviving
                    // conversation already agrees with the target.
                    
                    // Otherwise, rebuild the stack with Title as root,
                    // the selected conversation, and trailing
                    // Settings/About screens.
                    val restored =
                        if (lastSurvivingChat == targetActive) {
                            destinations
                        } else {
                            buildList<DroidMentorDestination> {
                                add(DroidMentorDestination.Title)

                                targetActive?.let { id ->
                                    add(
                                        DroidMentorDestination.ActiveChat(id)
                                    )
                                }

                                addAll(
                                    destinations.takeLastWhile {
                                        it == DroidMentorDestination.Settings ||
                                            it == DroidMentorDestination.About
                                    }
                                )
                            }
                        }

                    // Ensure the stack always has a safe Title root.
                    val normalized =
                        if (
                            restored.firstOrNull() ==
                            DroidMentorDestination.Title
                        ) {
                            restored
                        } else {
                            listOf(DroidMentorDestination.Title) + restored
                        }

                    DroidMentorNavigator(
                        initialBackStack = normalized,
                        repository = repository,
                    )
                },
            )
    }
}

@Composable
fun rememberDroidMentorNavigator(
    repository: ConversationRepository
): DroidMentorNavigator {
    return rememberSaveable(
        repository,
        saver = DroidMentorNavigator.saver(repository)
    ) {
        val resume = repository.resumableId()

        DroidMentorNavigator(
            initialBackStack =
                listOf(DroidMentorDestination.Title) +
                    listOfNotNull(
                        resume?.let {
                            DroidMentorDestination.ActiveChat(it)
                        }
                    ),
            repository = repository,
        )
    }
}
