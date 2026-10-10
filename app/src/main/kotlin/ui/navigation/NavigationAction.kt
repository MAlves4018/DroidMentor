
package ui.navigation

import domain.model.identity.ConversationId

sealed interface NavigationAction {

    data object NewChat : NavigationAction

    data object OpenTitle : NavigationAction

    data object OpenHistory : NavigationAction

    data class OpenConversation(
        val conversationId: ConversationId
    ) : NavigationAction

    data object OpenSettings : NavigationAction

    data object OpenAbout : NavigationAction

    data object Back : NavigationAction
}
