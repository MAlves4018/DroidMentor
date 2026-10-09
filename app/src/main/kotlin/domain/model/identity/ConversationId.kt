package domain.model.identity

data class ConversationId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "ConversationId cannot be blank."
        }
    }
}
