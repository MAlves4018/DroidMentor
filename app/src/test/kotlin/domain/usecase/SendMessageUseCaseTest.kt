package domain.usecase

import data.InMemorySettingsRepository
import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import test.fakes.ControlledMentorProvider

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SendMessageUseCaseTest {
    private val history = listOf(Message(MessageId("u1"), Role.USER, "Hello", 0))

    @Test fun readsCurrentKeyAndForwardsExactHistory() = runTest {
        val settings = InMemorySettingsRepository()
        settings.saveApiKey("key-for-test")
        val provider = ControlledMentorProvider()
        val result = async { SendMessageUseCase(provider, settings)(history) }
        runCurrent()
        assertEquals(1, provider.requests.size)
        assertEquals("key-for-test", provider.requests.single().apiKey)
        assertEquals(history, provider.requests.single().history)
        provider.succeed(0, "Reply")
        assertEquals("Reply", result.await())
    }

    @Test fun missingKeyNeverContactsProvider() = runTest {
        val provider = ControlledMentorProvider()
        val settings = InMemorySettingsRepository()
        val failure = runCatching { SendMessageUseCase(provider, settings)(history) }.exceptionOrNull()
        assertTrue(failure is ProviderFailure.Authentication)
        assertTrue(provider.requests.isEmpty())
    }
}
