package ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.common.DroidMentorLogo
import ui.common.DroidMentorTopBar

@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            DroidMentorTopBar(
                title = "About",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 20.dp,
                ),
        ) {
            DroidMentorLogo(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = "DroidMentor",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "WHAT IT IS",
                modifier = Modifier.padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "DroidMentor is an Android mentoring application developed for ISEL's Programação em Dispositivos Móveis course.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = "ACADEMIC PROJECT",
                modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Programação em Dispositivos Móveis\nISEL · 2026/27",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = "DEVELOPED BY",
                modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Miguel Alves\nMartim Gomes",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
