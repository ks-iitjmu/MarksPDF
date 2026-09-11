package com.example.lovepdf.feature.merge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.example.lovepdf.ui.components.WaveRingLoader

/**
 * Build a merge: add files, put them in the right order, run it.
 *
 * Order is shown as an explicit number on each row rather than left implicit in the
 * list position. Merge order is the one thing that silently produces a wrong result —
 * the output looks fine, just with chapters in the wrong sequence — so it's worth
 * spelling out.
 */
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    // Explicit tint: nothing above this sets LocalContentColor, so the
                    // default is black and vanishes in dark mode.
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Merge PDFs",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = onAddFiles) { Text("Add") }
                }
            }

            if (items.isEmpty()) {
                EmptyPicker(onAddFiles, Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = { namingFile = true },
                        enabled = items.size >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (items.size >= 2) {
                                "Merge ${items.size} files"
                            } else {
                                "Add one more file"
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = mergeState !is MergeState.Editing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            MergeStatus(state = mergeState, onDismiss = onDismissResult)
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

/**
 * Asks for a filename before the merge starts.
 *
 * Naming up front rather than through a system save dialog, because the app writes into
 * its own folder — the location isn't the user's decision, only the name is. Asking for
 * one thing in a small dialog beats handing over a full file browser to collect it.
 */
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
                    text = "Saves to Downloads / ${PdfOutputStore.FOLDER_NAME}",
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
            text = "Add the PDFs you want to combine",
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
private fun MergeStatus(
    state: MergeState,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is MergeState.Working -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WaveRingLoader()
                Text(
                    text = "Merging",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            is MergeState.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 20.dp),
                ) { Text("Back") }
            }

            else -> Unit
        }
    }
}
