package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared inline feedback surface for information, warnings, errors and stopped work. */
@Composable
fun InlineNotice(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    kind: NoticeKind = NoticeKind.INFO,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val background = when (kind) {
        NoticeKind.INFO -> Color(0xFFEFF6FF)
        NoticeKind.WARNING -> Color(0xFFFFF7ED)
        NoticeKind.ERROR -> Color(0xFFFEF2F2)
        NoticeKind.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when (kind) {
        NoticeKind.INFO -> Color(0xFF1E40AF)
        NoticeKind.WARNING -> Color(0xFF9A3412)
        NoticeKind.ERROR -> Color(0xFF991B1B)
        NoticeKind.STOPPED -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    color = foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = message,
                color = foreground,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    color = foreground,
                )
            }
        }
    }
}
