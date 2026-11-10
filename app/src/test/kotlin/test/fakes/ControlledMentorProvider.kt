package test.fakes

import domain.model.Message
import domain.provider.MentorProvider
import kotlinx.coroutines.CompletableDeferred

/** Each call suspends until the test decides how it ends. No HTTP requests. */
class ControlledMentorProvider : MentorProvider {
    data class Request(
        val history: List<Message>,
        val apiKey: String,
        val result: CompletableDeferred<String> = CompletableDeferred(),
    )

    val requests = mutableListOf<Request>()

    override suspend fun generateReply(history: List<Message>, apiKey: String): String {
        val request = Request(history.toList(), apiKey)
        requests += request
        return request.result.await()
    }

    fun succeed(index: Int, text: String) {
        check(requests[index].result.complete(text)) { "Request already completed" }
    }

    fun fail(index: Int, cause: Throwable) {
        check(requests[index].result.completeExceptionally(cause)) { "Request already completed" }
    }
}
