package ui.chat

object ActiveChatTestTags {
    const val EMPTY_CONTENT = "active_chat_empty_content"
    const val MESSAGE_LIST = "active_chat_message_list"
    const val COMPOSER_FIELD = "active_chat_composer_field"
    const val SEND_ACTION = "active_chat_send_action"
    const val STOP_ACTION = "active_chat_stop_action"
    const val ATTACH_ACTION = "active_chat_attach_action"
    const val ATTACHMENT_DRAFT = "active_chat_attachment_draft"
    const val REMOVE_ATTACHMENT_ACTION = "active_chat_remove_attachment_action"
    const val REASONING_SELECTOR = "active_chat_reasoning_selector"
    const val THINKING_INDICATOR = "active_chat_thinking_indicator"
    const val REQUEST_FEEDBACK = "active_chat_request_feedback"
    const val EDIT_FIELD = "active_chat_edit_field"
    const val EDIT_SAVE = "active_chat_edit_save"
    const val CONFIRM_REWRITE = "active_chat_confirm_rewrite"

    fun userMessage(id: String) = "active_chat_user_message_$id"
    fun modelMessage(id: String) = "active_chat_model_message_$id"
    fun messageActions(id: String) = "active_chat_message_actions_$id"
    fun editAction(id: String) = "active_chat_edit_action_$id"
}
