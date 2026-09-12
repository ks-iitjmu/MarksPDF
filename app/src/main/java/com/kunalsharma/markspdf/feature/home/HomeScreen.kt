package com.kunalsharma.markspdf.feature.home

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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.kunalsharma.markspdf.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.ui.components.PillHeader
import com.kunalsharma.markspdf.ui.components.PillIconButton
import com.kunalsharma.markspdf.ui.theme.pillSurfaceColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Tool(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
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
    onChangeLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = listOf(
        Tool(Icons.Outlined.FolderOpen, stringResource(R.string.tool_view_title), stringResource(R.string.tool_view_subtitle), onOpenFile),
        Tool(Icons.AutoMirrored.Outlined.MergeType, stringResource(R.string.tool_merge_title), stringResource(R.string.tool_merge_subtitle), onMerge),
        Tool(Icons.Outlined.ContentCut, stringResource(R.string.tool_split_title), stringResource(R.string.tool_split_subtitle), onSplit),
        Tool(Icons.Outlined.PhotoLibrary, stringResource(R.string.tool_images_title), stringResource(R.string.tool_images_subtitle), onImagesToPdf),
        Tool(Icons.Outlined.Image, stringResource(R.string.tool_export_title), stringResource(R.string.tool_export_subtitle), onPdfToImages),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            PillHeader(
                title = stringResource(R.string.app_name),
                showMark = true,
                leading = {
                    PillIconButton(
                        icon = Icons.Outlined.Translate,
                        label = stringResource(R.string.cd_change_language),
                        onClick = onChangeLanguage,
                    )
                },
                trailing = {
                    PillIconButton(
                        icon = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        label = stringResource(if (darkMode) R.string.cd_switch_light else R.string.cd_switch_dark),
                        onClick = onToggleDarkMode,
                        active = darkMode,
                    )
                },
            )
        }

        item {
            Text(
                text = stringResource(R.string.home_question),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.2f,
                modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
            )
        }

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

        item { SectionLabel(stringResource(R.string.section_recent), topPadding = 28.dp) }

        if (recents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recents_empty),
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

    Surface(
        onClick = tool.onClick,
        modifier = modifier.height(132.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
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
        color = pillSurfaceColor(),
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
                    text = supportingText(document, LocalContext.current),
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
                    contentDescription = stringResource(R.string.cd_remove_recent, document.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
private fun supportingText(document: RecentDocument, context: android.content.Context): String {
    val opened = formatOpenedAt(document.openedAt, context)
    if (document.pages <= 0) return opened
    val pages = context.resources.getQuantityString(
        R.plurals.page_count, document.pages, document.pages
    )
    return "$pages · $opened"
}

private val OuterCorner = 20.dp
private val InnerCorner = 4.dp
private val GroupGap = 3.dp

private fun formatOpenedAt(millis: Long, context: android.content.Context): String {
    val elapsed = System.currentTimeMillis() - millis
    return when {
        elapsed < 60_000 -> context.getString(R.string.time_just_now)
        elapsed < 3_600_000 ->
            context.getString(R.string.time_minutes_ago, (elapsed / 60_000).toInt())
        elapsed < 86_400_000 ->
            context.getString(R.string.time_hours_ago, (elapsed / 3_600_000).toInt())
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
