package com.example.lovepdf.feature.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.lovepdf.feature.tools.SplitMode
import com.example.lovepdf.feature.tools.SplitState
import com.example.lovepdf.ui.components.ExpressiveSlider
import com.example.lovepdf.ui.components.WaveRingLoader
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Split a PDF, with all three ways of doing it on one screen.
 *
 * The modes stay visible as a single list rather than being hidden behind a chooser,
 * because "split" means three different things to different people and the fastest way
 * to find the right one is to see them side by side. Each mode shows a live sentence of
 * what it will produce, so the outcome is known before anything is written.
 */
@Composable
fun SplitScreen(
    state: SplitState,
    mode: SplitMode,
    fromPage: Int,
    toPage: Int,
    parts: Int,
    suggestedName: String,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onModeChange: (SplitMode) -> Unit,
    onFromPageChange: (Int) -> Unit,
    onToPageChange: (Int) -> Unit,
    onPartsChange: (Int) -> Unit,
    onRun: (baseName: String) -> Unit,
    onOpenResult: (android.net.Uri) -> Unit,
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Split PDF",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (state is SplitState.Ready) {
                    TextButton(onClick = onPickFile) { Text("Change") }
                }
            }

            when (state) {
                is SplitState.NoFile -> EmptyPicker(onPickFile, Modifier.weight(1f))

                is SplitState.Loading -> Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { WaveRingLoader() }

                is SplitState.Ready -> {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${state.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                        )

                        ModeCard(
                            title = "Extract a range",
                            summary = "Makes one file with pages $fromPage to $toPage",
                            selected = mode == SplitMode.Range,
                            onClick = { onModeChange(SplitMode.Range) },
                        ) {
                            PageRangeControls(
                                pageCount = state.pageCount,
                                fromPage = fromPage,
                                toPage = toPage,
                                onFromPageChange = onFromPageChange,
                                onToPageChange = onToPageChange,
                            )
                        }

                        ModeCard(
                            title = "Every page separately",
                            summary = "Makes ${state.pageCount} files, one per page",
                            selected = mode == SplitMode.EveryPage,
                            onClick = { onModeChange(SplitMode.EveryPage) },
                        )

                        ModeCard(
                            title = "Into equal parts",
                            summary = partsSummary(state.pageCount, parts),
                            selected = mode == SplitMode.Parts,
                            onClick = { onModeChange(SplitMode.Parts) },
                        ) {
                            ExpressiveSlider(
                                value = parts.toFloat(),
                                // Snapping to whole parts happens here: the slider
                                // reports a continuous value and the ViewModel clamps it
                                // to a valid count.
                                onValueChange = { onPartsChange(it.roundToInt()) },
                                valueRange = 2f..state.pageCount.coerceAtLeast(2).toFloat(),
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
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Split") }
                    }
                }

                else -> Unit
            }
        }

        AnimatedVisibility(
            visible = state is SplitState.Working ||
                state is SplitState.Done ||
                state is SplitState.Failed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SplitStatus(state, onOpenResult, onDismissResult)
        }
    }

    if (namingFile) {
        NameFileDialog(
            initialName = suggestedName,
            onConfirm = {
                namingFile = false
                onRun(it)
            },
            onDismiss = { namingFile = false },
        )
    }
}

private fun partsSummary(pageCount: Int, parts: Int): String {
    val perFile = ceil(pageCount.toDouble() / parts).toInt().coerceAtLeast(1)
    return "Makes $parts files of about $perFile pages each"
}

@Composable
private fun ModeCard(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    controls: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
        Column(Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            // Controls only appear for the chosen mode, so the screen stays readable
            // instead of showing three sets of inputs at once.
            if (selected && controls != null) {
                Box(Modifier.padding(top = 12.dp)) { controls() }
            }
        }
    }
}

@Composable
private fun PageRangeControls(
    pageCount: Int,
    fromPage: Int,
    toPage: Int,
    onFromPageChange: (Int) -> Unit,
    onToPageChange: (Int) -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PageField("From", fromPage, pageCount, onFromPageChange, Modifier.weight(1f))
            PageField("To", toPage, pageCount, onToPageChange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PageField(
    label: String,
    value: Int,
    pageCount: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Text is held locally so the field can be empty mid-edit. Feeding every keystroke
    // straight into a clamped Int makes backspacing impossible — clearing "12" would
    // snap it back to 1 before the second digit could be typed.
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { entry ->
            val digits = entry.filter { it.isDigit() }.take(5)
            text = digits
            digits.toIntOrNull()?.let { onChange(it.coerceIn(1, pageCount)) }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun EmptyPicker(onPickFile: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onPickFile,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Choose a PDF",
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = "Choose the PDF you want to split",
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
        title = { Text("Name your files") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("File name") },
                )
                Text(
                    text = "Saves to Downloads / ${PdfOutputStore.FOLDER_NAME}. " +
                        "Multiple files get numbered automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Split")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SplitStatus(
    state: SplitState,
    onOpenResult: (android.net.Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is SplitState.Working -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WaveRingLoader()
                Text(
                    text = "Splitting",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            is SplitState.Done -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    text = if (state.results.size == 1) {
                        "Saved 1 file"
                    } else {
                        "Saved ${state.results.size} files"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "In Downloads / ${PdfOutputStore.FOLDER_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                    state.results.firstOrNull()?.let { first ->
                        // Opening every result isn't possible, so the first one stands
                        // in as a spot check that the split came out right.
                        Button(onClick = { onOpenResult(first) }) {
                            Text(if (state.results.size == 1) "Open it" else "Open first")
                        }
                    }
                }
            }

            is SplitState.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
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
