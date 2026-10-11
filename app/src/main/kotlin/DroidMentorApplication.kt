package isel.dei.pdm.droidmentor

import android.app.Application
import domain.repository.ConversationRepository
import domain.repository.InMemoryConversationRepository

class DroidMentorApplication : Application() {
    val conversations: ConversationRepository by lazy { InMemoryConversationRepository() }
}