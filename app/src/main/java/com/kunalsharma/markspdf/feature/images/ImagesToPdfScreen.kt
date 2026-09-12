package com.kunalsharma.markspdf.feature.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kunalsharma.markspdf.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.core.pdf.ImageDecoder
import com.kunalsharma.markspdf.core.pdf.PdfOutputStore
import com.kunalsharma.markspdf.core.pdf.PdfPageFit
import com.kunalsharma.markspdf.feature.tools.ImageItem
import com.kunalsharma.markspdf.feature.tools.ImagesToPdfState
import com.kunalsharma.markspdf.ui.components.ToolHeaderAction
import com.kunalsharma.markspdf.ui.components.ToolPrimaryButton
import com.kunalsharma.markspdf.ui.components.ToolScaffold
import com.kunalsharma.markspdf.ui.components.ToolStatusSurface
import com.kunalsharma.markspdf.ui.components.WaveRingLoader

@Composable
fun ImagesToPdfScreen(
    items: List<ImageItem>,
    fit: PdfPageFit,
    state: ImagesToPdfState,
    suggestedName: String,
    onBack: () -> Unit,
    onAddImages: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (index: Int, delta: Int) -> Unit,
    onFitChange: (PdfPageFit) -> Unit,
    onCreate: (fileName: String) -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var namingFile by remember { mutableStateOf(false) }

    ToolScaffold(
        title = stringResource(R.string.images_title),
        onBack = onBack,
        modifier = modifier,
        action = { if (items.isNotEmpty()) ToolHeaderAction(Icons.Outlined.Add, stringResource(R.string.cd_add_images), onAddImages) },
        bottomBar = {
            if (items.isNotEmpty()) {
                ToolPrimaryButton(
                    // Hyphenated so it reads "a 3-page PDF", not "a 3 pages PDF".
                    text = pluralStringResource(R.plurals.images_create_button, items.size, items.size),
                    onClick = { namingFile = true },
                )
            }
        },
        overlay = if (state !is ImagesToPdfState.Editing) {
            { StatusOverlay(state, onDismissResult) }
        } else {
            null
        },
    ) {
        if (items.isEmpty()) {
            AddImagesPrompt(onAddImages, Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FitSelector(fit = fit, onFitChange = onFitChange) }

                items(items.size, key = { items[it].uri.toString() }) { index ->
                    ImageRow(
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
            onConfirm = {
                namingFile = false
                onCreate(it)
            },
            onDismiss = { namingFile = false },
        )
    }
}

@Composable
private fun FitSelector(fit: PdfPageFit, onFitChange: (PdfPageFit) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FitOption(
            title = stringResource(R.string.fit_match_title),
            summary = stringResource(R.string.fit_match_summary),
            selected = fit == PdfPageFit.MatchImage,
            onClick = { onFitChange(PdfPageFit.MatchImage) },
            modifier = Modifier.weight(1f),
        )
        FitOption(
            title = stringResource(R.string.fit_a4_title),
            summary = stringResource(R.string.fit_a4_summary),
            selected = fit == PdfPageFit.A4,
            onClick = { onFitChange(PdfPageFit.A4) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FitOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ImageRow(
    position: Int,
    item: ImageItem,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var preview by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri) {
        preview = ImageDecoder.decode(context, item.uri, maxDimension = 160)?.asImageBitmap()
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                preview?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Text(
                text = "$position",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )

            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.cd_move_earlier))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.cd_move_later))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.cd_remove))
            }
        }
    }
}

@Composable
private fun AddImagesPrompt(onAddImages: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onAddImages,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.cd_add_images),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.images_empty_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
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
        title = { Text(stringResource(R.string.dialog_name_file)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.field_file_name)) },
                )
                Text(
                    text = stringResource(R.string.saved_to_folder, PdfOutputStore.FOLDER_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun StatusOverlay(state: ImagesToPdfState, onDismiss: () -> Unit) {
    ToolStatusSurface {
        when (state) {
            is ImagesToPdfState.Working -> {
                WaveRingLoader()
                Text(
                    text = stringResource(R.string.images_progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            is ImagesToPdfState.Failed -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp)) {
                    Text(stringResource(R.string.action_back))
                }
            }

            else -> Unit
        }
    }
}
