package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ui.theme.DroidMentorUiTokens

@Composable
fun DroidMentorTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onNewChat: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DroidMentorUiTokens.AppBarHeight)
            .background(MaterialTheme.colorScheme.background),
    ) {

        when {
            onMenu != null -> {
                HeaderButton(
                    icon = Icons.Filled.Menu,
                    description = "Open chat history",
                    onClick = onMenu,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }

            onBack != null -> {
                HeaderButton(
                    icon = Icons.Filled.ArrowBack,
                    description = "Back",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.66f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (onNewChat != null) {
            HeaderButton(
                icon = Icons.Filled.Add,
                description = "New chat",
                onClick = onNewChat,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}