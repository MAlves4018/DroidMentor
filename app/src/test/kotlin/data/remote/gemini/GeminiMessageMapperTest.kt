package data.remote.gemini

import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GeminiMessageMapper.
 *
 * Verifies:
 * - Correct role mapping.
 * - Complete conversation history.
 * - Chronological message ordering.
 * - Android mentor system instruction.
 * - Correct JSON serialization.
 *
 * These tests do not require Android or network access.
 */
class GeminiMessageMapperTest {

    private val json = Json {
        explicitNulls = false
        encodeDefaults = false
    }

    /**
     * Creates a valid domain message for testing.
     */
    private fun message(
        id: String,
        role: Role,
        content: String,
        sequence: Long
    ): Message = Message(
        id = MessageId(id),
        role = role,
        content = content,
        sequence = sequence
    )

    @Test
    fun `user role is mapped to Gemini user`() {
        val history = listOf(
            message("1", Role.USER, "Hello", 0)
        )

        val request = GeminiMessageMapper.toRequest(history)

        assertEquals("user", request.contents[0].role)
        assertEquals("Hello", request.contents[0].parts[0].text)
    }

    @Test
    fun `model role is mapped to Gemini model`() {
        val history = listOf(
            message("1", Role.MODEL, "Hello!", 0)
        )

        val request = GeminiMessageMapper.toRequest(history)

        assertEquals("model", request.contents[0].role)
        assertEquals("Hello!", request.contents[0].parts[0].text)
    }

    @Test
    fun `complete conversation history is included`() {
        val history = listOf(
            message("1", Role.USER, "What is a ViewModel?", 0),
            message("2", Role.MODEL, "It manages UI state.", 1),
            message("3", Role.USER, "Give me an example.", 2),
            message("4", Role.MODEL, "Here is an example.", 3),
            message("5", Role.USER, "What about StateFlow?", 4)
        )

        val request = GeminiMessageMapper.toRequest(history)

        assertEquals(5, request.contents.size)

        assertEquals(
            listOf("user", "model", "user", "model", "user"),
            request.contents.map { it.role }
        )

        assertEquals(
            history.map { it.content },
            request.contents.map { it.parts[0].text }
        )
    }

    @Test
    fun `messages are sorted by sequence`() {
        val history = listOf(
            message("3", Role.USER, "Third", 2),
            message("1", Role.USER, "First", 0),
            message("2", Role.MODEL, "Second", 1)
        )

        val request = GeminiMessageMapper.toRequest(history)

        assertEquals(
            listOf("First", "Second", "Third"),
            request.contents.map { it.parts[0].text }
        )
    }

    @Test
    fun `system instruction configures Android mentor`() {
        val history = listOf(
            message("1", Role.USER, "Hello", 0)
        )

        val request = GeminiMessageMapper.toRequest(history)

        val instruction = request.systemInstruction
            .parts
            .joinToString(" ") { it.text }

        assertTrue(
            instruction.contains("Android", ignoreCase = true)
        )

        assertTrue(
            instruction.contains("mentor", ignoreCase = true)
        )

        assertTrue(
            instruction.contains("Kotlin", ignoreCase = true)
        )
    }

    @Test
    fun `system instruction does not have a conversation role`() {
        val request = GeminiMessageMapper.toRequest(
            listOf(message("1", Role.USER, "Hello", 0))
        )

        assertEquals(null, request.systemInstruction.role)
    }

    @Test
    fun `request serializes to correct Gemini JSON structure`() {
        val history = listOf(
            message("1", Role.USER, "Hello", 0),
            message("2", Role.MODEL, "Hi!", 1)
        )

        val request = GeminiMessageMapper.toRequest(history)

        val payload = json.parseToJsonElement(
            json.encodeToString(request)
        ).jsonObject

        assertTrue(payload.containsKey("system_instruction"))
        assertTrue(payload.containsKey("contents"))

        val systemInstruction =
            payload.getValue("system_instruction").jsonObject

        assertFalse(systemInstruction.containsKey("role"))

        val contents = payload.getValue("contents").jsonArray

        assertEquals(2, contents.size)

        val firstMessage = contents[0].jsonObject

        assertEquals(
            "user",
            firstMessage.getValue("role").jsonPrimitive.content
        )

        val parts = firstMessage.getValue("parts").jsonArray

        assertEquals(
            "Hello",
            parts[0].jsonObject
                .getValue("text").jsonPrimitive.content
        )

        val secondMessage = contents[1].jsonObject

        assertEquals(
            "model",
            secondMessage.getValue("role").jsonPrimitive.content
        )
    }

    @Test
    fun `mapping does not modify original history`() {
        val history = mutableListOf(
            message("2", Role.MODEL, "Second", 1),
            message("1", Role.USER, "First", 0)
        )

        GeminiMessageMapper.toRequest(history)

        assertEquals("Second", history[0].content)
        assertEquals("First", history[1].content)
    }
}
