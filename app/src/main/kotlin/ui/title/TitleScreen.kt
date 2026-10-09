package ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.common.DroidMentorLogo
import ui.common.DroidMentorTopBar
import ui.common.MessageInputBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag

@Composable
fun TitleScreen(
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var message by remember {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            DroidMentorTopBar(
                title = "DroidMentor",
                onMenu = onOpenHistory,
                onNewChat = onNewChat,
            )
        },

        bottomBar = {
            MessageInputBar(
                value = message,
                onValueChange = {
                    message = it
                },
                onSend = {
                    // Message sending will be implemented later
                },
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp,
                ),
            )
        },

        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.Center,
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                DroidMentorLogo()

                Spacer(
                    modifier = Modifier.height(22.dp),
                )

                Text(
                    text = "DroidMentor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = "How can I help you?",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag(TitleTestTags.SETTINGS_BUTTON)
                    ) {
                        Text("Settings")
                    }

                    TextButton(
                        onClick = onOpenAbout,
                        modifier = Modifier.testTag(TitleTestTags.ABOUT_BUTTON)
                    ) {
                        Text("About")
                    }
                }
            }
        }
    }
}