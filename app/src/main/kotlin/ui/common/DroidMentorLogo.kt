package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.theme.DroidMentorUiTokens

@Composable
fun DroidMentorLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val size =
        if (compact) 46.dp
        else DroidMentorUiTokens.HeroLogo

    val radius =
        if (compact) 14.dp
        else DroidMentorUiTokens.HeroLogoRadius

    Box(
        modifier = modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(radius)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "DM",
            color = MaterialTheme.colorScheme.background,
            fontWeight = FontWeight.ExtraBold,
            style =
                if (compact)
                    MaterialTheme.typography.labelLarge
                else
                    MaterialTheme.typography.titleMedium,
        )
    }
}