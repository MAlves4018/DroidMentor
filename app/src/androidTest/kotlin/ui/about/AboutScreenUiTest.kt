package ui.about

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

class AboutScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun presentsDurableProjectAndTeamInformation() {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                AboutScreen(onBack = {})
            }
        }

        composeRule
            .onNodeWithText("WHAT IT IS")
            .assertExists()

        composeRule
            .onNodeWithText("ACADEMIC PROJECT")
            .assertExists()

        composeRule
            .onNodeWithText("ISEL · 2026/27", substring = true)
            .assertExists()

        composeRule
            .onNodeWithText("DEVELOPED BY")
            .assertExists()

        composeRule
            .onNodeWithText("Miguel Alves", substring = true)
            .assertExists()

        composeRule
            .onNodeWithText("Martim Gomes", substring = true)
            .assertExists()
    }
}
