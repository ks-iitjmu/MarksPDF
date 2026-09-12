package com.example.lovepdf.feature.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.example.lovepdf.ui.text.countOf
import com.example.lovepdf.ui.components.ToolHeaderAction
import com.example.lovepdf.ui.components.ToolPrimaryButton
import com.example.lovepdf.ui.components.ToolScaffold
import com.example.lovepdf.ui.components.ToolStatusSurface
import com.example.lovepdf.ui.components.WaveRingLoader
import kotlin.math.ceil
import kotlin.math.roundToInt

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

    ToolScaffold(
        title = "Split PDF",
        onBack = onBack,
        modifier = modifier,
        action = { if (state is SplitState.Ready) ToolHeaderAction(Icons.Outlined.FolderOpen, "Change file", onPickFile) },
        bottomBar = {
            if (state is SplitState.Ready) {
                ToolPrimaryButton(
                    text = when (mode) {
                        SplitMode.Range ->
                            "Extract ${countOf(toPage - fromPage + 1, "page")}"
                        SplitMode.EveryPage ->
                            "Split into ${countOf(state.pageCount, "file")}"
                        SplitMode.Parts ->
                            "Split into ${countOf(parts, "file")}"
                    },
                    onClick = { namingFile = true },
                )
            }
        },
        overlay = when (state) {
            is SplitState.Working, is SplitState.Done, is SplitState.Failed ->
                { { SplitStatus(state, onOpenResult, onDismissResult) } }
            else -> null
        },
    ) {
        when (state) {
            is SplitState.NoFile -> EmptyPicker(onPickFile, Modifier.weight(1f))

            is SplitState.Loading -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { WaveRingLoader() }

            is SplitState.Ready -> Column(
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
                    text = countOf(state.pageCount, "page"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )

                ModeCard(
                    title = "Extract a page range",
                    summary = "Creates one file containing pages $fromPage to $toPage.",
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
                    summary = "Creates ${countOf(state.pageCount, "file")}, one per page.",
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
                        onValueChange = { onPartsChange(it.roundToInt()) },
                        valueRange = 2f..state.pageCount.coerceAtLeast(2).toFloat(),
                    )
                }
            }

            else -> Unit
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
            text = "Choose the PDF you want to split.",
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
                    text = "Saved to Downloads/${PdfOutputStore.FOLDER_NAME}. " +
                        "Multiple files are numbered automatically.",
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
    ToolStatusSurface {
        when (state) {
            is SplitState.Working -> {
                WaveRingLoader()
                Text(
                    text = "Splitting document",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            is SplitState.Done -> {
                Text(
                    text = "Saved ${countOf(state.results.size, "file")}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "In Downloads/${PdfOutputStore.FOLDER_NAME}",
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
                        Button(onClick = { onOpenResult(first) }) {
                            Text(if (state.results.size == 1) "Open it" else "Open first")
                        }
                    }
                }
            }

            is SplitState.Failed -> {
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
