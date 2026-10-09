package domain.model.identity

data class MessageId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "MessageId cannot be blank."
        }
    }
}
