package com.example.lovepdf.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lovepdf.ui.components.PillHeader
import com.example.lovepdf.ui.components.PillIconButton
import com.example.lovepdf.ui.theme.ToolAccent
import com.example.lovepdf.ui.theme.toolCardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Tool(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: ToolAccent,
    val onClick: () -> Unit,
)

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
    onImagesToPdf: () -> Unit,
    onPdfToImages: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = listOf(
        Tool(Icons.Outlined.FolderOpen, "View PDF", "Open and start reading", ToolAccent.Violet, onOpenFile),
        Tool(Icons.AutoMirrored.Outlined.MergeType, "Merge PDFs", "Combine into one file", ToolAccent.Rose, onMerge),
        Tool(Icons.Outlined.ContentCut, "Split PDF", "Extract or divide pages", ToolAccent.Mint, onSplit),
        Tool(Icons.Outlined.PhotoLibrary, "Images to PDF", "Combine photos or scans", ToolAccent.Sky, onImagesToPdf),
        Tool(Icons.Outlined.Image, "PDF to images", "Extract pages as images", ToolAccent.Amber, onPdfToImages),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            PillHeader(
                title = "LovePDF",
                showMark = true,
                leading = {
                    PillIconButton(
                        icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        label = "Exit app",
                        onClick = onExitApp,
                    )
                },
                trailing = {
                    PillIconButton(
                        icon = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        label = if (darkMode) "Switch to light theme" else "Switch to dark theme",
                        onClick = onToggleDarkMode,
                        active = darkMode,
                    )
                },
            )
        }

        item {
            Text(
                text = "What would you\nlike to do?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.15f,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 22.dp),
            )
        }

        item { SectionLabel("All tools") }

        items(tools.chunked(2).size) { rowIndex ->
            val row = tools.chunked(2)[rowIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { tool ->
                    ToolCard(tool = tool, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }

        item { SectionLabel("Recent", topPadding = 28.dp) }

        if (recents.isEmpty()) {
            item {
                Text(
                    text = "Files you open will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(recents.size, key = { recents[it].uri.toString() }) { index ->
                RecentRow(
                    document = recents[index],
                    position = groupPosition(index, recents.size),
                    onClick = { onOpenRecent(recents[index]) },
                    onForget = { onForgetRecent(recents[index]) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 10.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = 10.dp),
    )
}

@Composable
private fun ToolCard(tool: Tool, modifier: Modifier = Modifier) {
    val colors = toolCardColors(tool.accent)

    Surface(
        onClick = tool.onClick,
        modifier = modifier.height(132.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = colors.onContainer,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.support,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class GroupPosition { Single, First, Middle, Last }

private fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count == 1 -> GroupPosition.Single
    index == 0 -> GroupPosition.First
    index == count - 1 -> GroupPosition.Last
    else -> GroupPosition.Middle
}

@Composable
private fun RecentRow(
    document: RecentDocument,
    position: GroupPosition,
    onClick: () -> Unit,
    onForget: () -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (position == GroupPosition.First || position == GroupPosition.Single) OuterCorner else InnerCorner,
        topEnd = if (position == GroupPosition.First || position == GroupPosition.Single) OuterCorner else InnerCorner,
        bottomStart = if (position == GroupPosition.Last || position == GroupPosition.Single) OuterCorner else InnerCorner,
        bottomEnd = if (position == GroupPosition.Last || position == GroupPosition.Single) OuterCorner else InnerCorner,
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = GroupGap / 2),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supportingText(document),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onForget,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove ${document.name} from recents",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
private fun supportingText(document: RecentDocument): String {
    val opened = formatOpenedAt(document.openedAt)
    return if (document.pages > 0) "${document.pages} pages · $opened" else opened
}

private val OuterCorner = 20.dp
private val InnerCorner = 4.dp
private val GroupGap = 3.dp

private fun formatOpenedAt(millis: Long): String {
    val elapsed = System.currentTimeMillis() - millis
    return when {
        elapsed < 60_000 -> "Just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000} hr ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
