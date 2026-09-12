package com.kunalsharma.markspdf.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.ui.theme.pillButtonColor
import com.kunalsharma.markspdf.ui.theme.pillSurfaceColor

@Composable
fun PillHeader(
    title: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showMark: Boolean = false,
) {
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = pillSurfaceColor(),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()

            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showMark) {
                    AppLogo(size = 20.dp)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = if (showMark) 8.dp else 0.dp),
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            trailing()
        }
    }
}

@Composable
fun PillIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val width by animateDpAsState(
        targetValue = if (active) 52.dp else ButtonSize,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillButtonWidth",
    )
    val corner by animateDpAsState(
        targetValue = if (active) 16.dp else ButtonSize / 2,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillButtonCorner",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.size(width = width, height = ButtonSize),
        shape = RoundedCornerShape(corner),
        color = if (active) MaterialTheme.colorScheme.primary else pillButtonColor(),
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun PillButtonSpacer() {
    Box(Modifier.size(ButtonSize))
}

private val ButtonSize = 40.dp
