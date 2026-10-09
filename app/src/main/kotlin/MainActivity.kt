package isel.dei.pdm.droidmentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ui.theme.DroidMentorTheme
import ui.title.TitleScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DroidMentorTheme(
                darkTheme = false
            ) {
                TitleScreen(
                    onNewChat = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                    onOpenAbout = {},
                )
            }
        }
    }
}