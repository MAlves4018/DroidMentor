package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SendReadyBlue,
    error = StopRed,

    background = DroidMentorLightBackground,
    surface = DroidMentorLightSurface,
    surfaceVariant = DroidMentorLightSurfaceVariant,

    outline = DroidMentorLightBorder,
    outlineVariant = DroidMentorLightBorder,

    onBackground = DroidMentorLightInk,
    onSurface = DroidMentorLightInk,
    onSurfaceVariant = DroidMentorLightMuted,
)

private val DarkColors = darkColorScheme(
    primary = SendReadyBlue,
    error = StopRed,

    background = DroidMentorDarkBackground,
    surface = DroidMentorDarkSurface,
    surfaceVariant = DroidMentorDarkSurfaceVariant,

    outline = DroidMentorDarkBorder,
    outlineVariant = DroidMentorDarkBorder,

    onBackground = DroidMentorDarkInk,
    onSurface = DroidMentorDarkInk,
    onSurfaceVariant = DroidMentorDarkMuted,
)

@Composable
fun DroidMentorTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DroidMentorTypography,
        content = content,
    )
}