package platform.navigation

import domain.model.identity.ConversationId

sealed interface DroidMentorDestination {

    data object Title : DroidMentorDestination

    data class ActiveChat(
        val conversationId: ConversationId
    ) : DroidMentorDestination

    data object Settings : DroidMentorDestination

    data object About : DroidMentorDestination
}
