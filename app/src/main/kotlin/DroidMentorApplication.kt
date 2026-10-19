package isel.dei.pdm.droidmentor

import android.app.Application

import domain.repository.ConversationRepository
import domain.repository.InMemoryConversationRepository
import domain.repository.SettingsRepository
import domain.provider.MentorProvider

import data.InMemorySettingsRepository
import data.UnavailableMentorProvider

class DroidMentorApplication : Application() {

    val conversations: ConversationRepository by lazy {
        InMemoryConversationRepository()
    }

    val settings: SettingsRepository by lazy {
        InMemorySettingsRepository()
    }

    val mentor: MentorProvider by lazy {
        UnavailableMentorProvider()
    }
}