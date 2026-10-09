package ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import ui.common.MentorScaffold
import ui.common.MessageInputBar

@Composable
fun TitleScreen(
    onOpenHistory: () -> Unit,
) {
    var message by remember {
        mutableStateOf("")
    }

    MentorScaffold(
        title = "DroidMentor",
        onMenu = onOpenHistory,
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
        }
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
            }
        }
    }
}