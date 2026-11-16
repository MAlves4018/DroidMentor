package platform.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import isel.dei.pdm.droidmentor.DroidMentorApplication
import isel.dei.pdm.droidmentor.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import ui.chat.ActiveChatTestTags

/**
 * DB-002: exercises the actual Activity, Room repository, History and Active Chat.
 * No Send action is dispatched; provider/network behavior is tested separately.
 * An offline process-restart check is documented as a manual device gate.
 */
class DB002PersistedHistoryUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repository
        get() = (composeRule.activity.application as DroidMentorApplication).conversations

    private val createdIds = mutableListOf<ConversationId>()
    private var originalActive: ConversationId? = null
    private var seeded = false

    private data class Fixture(
        val firstId: ConversationId,
        val secondId: ConversationId,
        val firstTitle: String,
        val secondTitle: String,
        val firstAnswer: String,
        val secondAnswer: String,
    )

    private fun seedTwoConversations(): Fixture {
        // Wait until the first navigation initialization has finished.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Open chat history")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        val suffix = UUID.randomUUID().toString().take(8)
        val firstTitle = "DB002 A $suffix"
        val secondTitle = "DB002 B $suffix"
        val firstAnswer = "Stored answer A $suffix"
        val secondAnswer = "Stored answer B $suffix"

        return runBlocking {
            repository.initialize()
            originalActive = repository.activeId
            seeded = true

            suspend fun savePair(title: String, answer: String): ConversationId {
                val created = repository.create()
                createdIds += created.id
                repository.save(
                    created
                        .append(
                            Message(
                                id = MessageId(UUID.randomUUID().toString()),
                                role = Role.USER,
                                content = title,
                                sequence = 0,
                            )
                        )
                        .append(
                            Message(
                                id = MessageId(UUID.randomUUID().toString()),
                                role = Role.MODEL,
                                content = answer,
                                sequence = 1,
                            )
                        )
                )
                return created.id
            }

            val first = savePair(firstTitle, firstAnswer)
            val second = savePair(secondTitle, secondAnswer)
            Fixture(first, second, firstTitle, secondTitle, firstAnswer, secondAnswer)
        }
    }

    @After
    fun cleanUpFixtures() {
        runBlocking {
            if (!seeded) return@runBlocking
            repository.initialize()
            createdIds.forEach { id ->
                if (repository.find(id) != null) repository.delete(id)
            }
            val original = originalActive
            if (original != null && repository.find(original) != null) {
                repository.activate(original)
            } else {
                repository.leaveActive()
            }
        }
    }

    private fun waitForText(value: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun historyListMatcher() =
        hasScrollToKeyAction() and !hasTestTag(ActiveChatTestTags.MESSAGE_LIST)

    private fun historyConversation(title: String) =
        hasText(title) and hasClickAction() and hasAnyAncestor(historyListMatcher())

    private fun scrollToHistoryConversation(id: ConversationId) {
        // Both History and Active Chat have keyed LazyColumns. Active Chat's
        // list is already tagged, so exclude it without modifying production.
        composeRule.onNode(historyListMatcher()).performScrollToKey(id.value)
    }

    private fun recreateWithStoredSelection() {
        // The Activity may have saved Title before the test created conversations.
        // The restored Navigator must reconcile that stale route with Room.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    private fun openHistory() {
        composeRule.onNodeWithContentDescription("Open chat history").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun storedMessagesPopulateHistoryAndActiveChatAfterRecreation() {
        val f = seedTwoConversations()
        recreateWithStoredSelection()

        waitForText(f.secondAnswer)
        composeRule.onNodeWithText(f.secondAnswer).assertIsDisplayed()
        assertEquals(f.secondId, repository.activeId)

        openHistory()
        scrollToHistoryConversation(f.secondId)
        composeRule.onNode(historyConversation(f.secondTitle)).assertIsDisplayed()
        scrollToHistoryConversation(f.firstId)
        composeRule.onNode(historyConversation(f.firstTitle)).assertIsDisplayed()

        composeRule.onNode(historyConversation(f.firstTitle)).performClick()
        waitForText(f.firstAnswer)
        composeRule.onNodeWithText(f.firstAnswer).assertIsDisplayed()
        assertEquals(f.firstId, repository.activeId)
    }

    @Test
    fun deletingActiveChatFromHistoryKeepsTheOtherConversation() {
        val f = seedTwoConversations()
        recreateWithStoredSelection()
        waitForText(f.secondAnswer)

        openHistory()
        scrollToHistoryConversation(f.secondId)
        composeRule.onNode(historyConversation(f.secondTitle)).assertIsDisplayed()

        // Select the options button inside the row identified by its title,
        // never by a global index (LazyColumn composes only visible rows).
        composeRule.onNode(
            hasContentDescription("Conversation options") and
                hasAnyAncestor(historyConversation(f.secondTitle))
        ).performClick()

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete conversation?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            repository.find(f.secondId) == null &&
                repository.activeId == null &&
                composeRule.onAllNodesWithText(f.secondTitle)
                    .fetchSemanticsNodes().isEmpty()
        }
        assertNull(repository.find(f.secondId))
        assertNotNull(repository.find(f.firstId))
        assertEquals(2, repository.find(f.firstId)!!.messages.size)

        // The surviving conversation must still be accessible through History.
        composeRule.onNode(historyConversation(f.firstTitle)).performClick()
        waitForText(f.firstAnswer)
        composeRule.onNodeWithText(f.firstAnswer).assertIsDisplayed()
        assertEquals(f.firstId, repository.activeId)
    }
}
