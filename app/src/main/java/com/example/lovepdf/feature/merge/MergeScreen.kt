package com.example.lovepdf.feature.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lovepdf.core.pdf.PdfOutputStore
import com.example.lovepdf.feature.tools.MergeItem
import com.example.lovepdf.feature.tools.MergeState
import com.example.lovepdf.ui.components.ToolHeaderAction
import com.example.lovepdf.ui.components.ToolPrimaryButton
import com.example.lovepdf.ui.components.ToolScaffold
import com.example.lovepdf.ui.components.ToolStatusSurface
import com.example.lovepdf.ui.components.WaveRingLoader
import com.example.lovepdf.ui.text.countOf

@Composable
fun MergeScreen(
    items: List<MergeItem>,
    mergeState: MergeState,
    onBack: () -> Unit,
    onAddFiles: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (index: Int, delta: Int) -> Unit,
    suggestedName: String,
    onStartMerge: (fileName: String) -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var namingFile by remember { mutableStateOf(false) }

    ToolScaffold(
        title = "Merge PDFs",
        icon = Icons.AutoMirrored.Outlined.MergeType,
        onBack = onBack,
        modifier = modifier,
        action = { if (items.isNotEmpty()) ToolHeaderAction(Icons.Outlined.Add, "Add files", onAddFiles) },
        bottomBar = {
            if (items.isNotEmpty()) {
                ToolPrimaryButton(
                    text = if (items.size >= 2) "Merge ${countOf(items.size, "file")}" else "Add one more file",
                    onClick = { namingFile = true },
                    enabled = items.size >= 2,
                )
            }
        },
        overlay = if (mergeState !is MergeState.Editing) {
            { MergeStatus(state = mergeState, onDismiss = onDismissResult) }
        } else {
            null
        },
    ) {
        if (items.isEmpty()) {
            EmptyPicker(onAddFiles, Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.size, key = { items[it].uri.toString() }) { index ->
                    FileRow(
                        position = index + 1,
                        item = items[index],
                        isFirst = index == 0,
                        isLast = index == items.lastIndex,
                        onMoveUp = { onMove(index, -1) },
                        onMoveDown = { onMove(index, 1) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
        }
    }

    if (namingFile) {
        NameFileDialog(
            initialName = suggestedName,
            onConfirm = { name ->
                namingFile = false
                onStartMerge(name)
            },
            onDismiss = { namingFile = false },
        )
    }
}

@Composable
private fun NameFileDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name your file") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("File name") },
                )
                Text(
                    text = "Saved to Downloads/${PdfOutputStore.FOLDER_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Merge") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyPicker(onAddFiles: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onAddFiles,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add PDFs",
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = "Add the PDFs you want to combine.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun FileRow(
    position: Int,
    item: MergeItem,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$position", style = MaterialTheme.typography.labelMedium)
                }
            }

            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )

            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Outlined.KeyboardArrowUp, "Move ${item.name} earlier")
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Outlined.KeyboardArrowDown, "Move ${item.name} later")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, "Remove ${item.name}")
            }
        }
    }
}

@Composable
private fun MergeStatus(state: MergeState, onDismiss: () -> Unit) {
    ToolStatusSurface {
        when (state) {
            is MergeState.Working -> {
                WaveRingLoader()
                Text(
                    text = "Merging files",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            is MergeState.Failed -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp)) {
                    Text("Back")
                }
            }

            else -> Unit
        }
    }
}
