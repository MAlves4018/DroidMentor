package ui.chat.model

/** One-shot feedback. Not persisted in conversation state. */
data class ConnectivityNotice(val online: Boolean, val manualCheck: Boolean)
