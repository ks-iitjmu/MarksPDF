package com.example.lovepdf.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The app's front door: what you can do, and what you were last doing.
 *
 * Tools come first because they're the reason to open the app deliberately; recents sit
 * below because reaching them usually means returning to something. A tool that doesn't
 * exist yet isn't shown as a greyed-out card — a disabled card promises something and
 * then refuses, which is worse than not mentioning it.
 */
@Composable
fun HomeScreen(
    recents: List<RecentDocument>,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenRecent: (RecentDocument) -> Unit,
    onForgetRecent: (RecentDocument) -> Unit,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Love PDF",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // Tints are set explicitly. These icons sit directly on the screen
                // background with no Surface above them, so LocalContentColor is never
                // set and falls back to black — which disappears in dark mode.
                IconButton(onClick = onShareApp) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Love PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (darkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionLabel("Tools")
        }

        item {
            ToolCard(
                icon = Icons.Outlined.FolderOpen,
                title = "View PDFs",
                subtitle = "Open a file and start reading",
                onClick = onOpenFile,
                prominent = true,
            )
        }

        item {
            ToolCard(
                icon = Icons.Outlined.MergeType,
                title = "Merge PDFs",
                subtitle = "Combine several files into one",
                onClick = onMerge,
            )
        }

        item {
            ToolCard(
                icon = Icons.Outlined.ContentCut,
                title = "Split PDF",
                subtitle = "Extract pages or break a file apart",
                onClick = onSplit,
            )
        }

        item {
            SectionLabel("Recent", topPadding = 28.dp)
        }

        if (recents.isEmpty()) {
            item {
                Text(
                    text = "Files you open will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(recents.size, key = { recents[it].uri.toString() }) { index ->
                RecentRow(
                    document = recents[index],
                    onClick = { onOpenRecent(recents[index]) },
                    onForget = { onForgetRecent(recents[index]) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 8.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = 10.dp),
    )
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    prominent: Boolean = false,
) {
    // Reading is the common case, so its card carries the stronger colour. Giving every
    // card equal weight would leave the user reading all of them each time.
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (prominent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (prominent) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (prominent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (prominent) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    document: RecentDocument,
    onClick: () -> Unit,
    onForget: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.PictureAsPdf,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatOpenedAt(document.openedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onForget) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${document.name} from recents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatOpenedAt(millis: Long): String {
    val now = System.currentTimeMillis()
    val elapsed = now - millis
    return when {
        elapsed < 60_000 -> "Just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000} hr ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
