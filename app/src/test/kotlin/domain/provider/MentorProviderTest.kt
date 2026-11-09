
package domain.provider

import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MentorProviderTest {

    private val history = listOf(
        Message(
            id = MessageId("message-1"),
            role = Role.USER,
            content = "What is a ViewModel?",
            sequence = 0L
        )
    )

    private val apiKey = "fake-test-key"

    @Test
    fun `generateResponse emits completed event with provider reply`() = runTest {
        val provider = FakeMentorProvider(
            reply = "A ViewModel manages UI state."
        )

        val events = provider.generateResponse(
            history = history,
            apiKey = apiKey
        ).toList()

        assertEquals(
            listOf(
                MentorResponse.Completed(
                    "A ViewModel manages UI state."
                )
            ),
            events
        )
    }

    @Test
    fun `generateResponse passes history and API key unchanged`() = runTest {
        val provider = FakeMentorProvider(
            reply = "Hello!"
        )

        provider.generateResponse(
            history = history,
            apiKey = apiKey
        ).toList()

        assertEquals(history, provider.receivedHistory)
        assertEquals(apiKey, provider.receivedApiKey)
    }

    @Test
    fun `generateResponse is cold and does not call provider until collected`() = runTest {
        val provider = FakeMentorProvider(
            reply = "Hello!"
        )

        val responseFlow = provider.generateResponse(
            history = history,
            apiKey = apiKey
        )

        assertEquals(0, provider.callCount)

        responseFlow.toList()

        assertEquals(1, provider.callCount)
    }

    @Test
    fun `generateResponse propagates provider failures`() = runTest {
        val provider = FakeMentorProvider(
            failure = ProviderFailure.Authentication
        )

        val failure = assertThrows(
            ProviderFailure.Authentication::class.java
        ) {
            kotlinx.coroutines.runBlocking {
                provider.generateResponse(
                    history = history,
                    apiKey = apiKey
                ).toList()
            }
        }

        assertEquals(
            ProviderFailure.Authentication,
            failure
        )
    }

    @Test
    fun `generateResponse preserves coroutine cancellation`() = runTest {
        val provider = FakeMentorProvider(
            failure = CancellationException("Cancelled")
        )

        val failure = assertThrows(
            CancellationException::class.java
        ) {
            kotlinx.coroutines.runBlocking {
                provider.generateResponse(
                    history = history,
                    apiKey = apiKey
                ).toList()
            }
        }

        assertEquals("Cancelled", failure.message)
    }

    private class FakeMentorProvider(
        private val reply: String = "",
        private val failure: Throwable? = null
    ) : MentorProvider {

        var receivedHistory: List<Message>? = null
            private set

        var receivedApiKey: String? = null
            private set

        var callCount: Int = 0
            private set

        override suspend fun generateReply(
            history: List<Message>,
            apiKey: String
        ): String {
            callCount++

            receivedHistory = history
            receivedApiKey = apiKey

            failure?.let { throw it }

            return reply
        }
    }
}


