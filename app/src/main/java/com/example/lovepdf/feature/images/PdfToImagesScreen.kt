package com.example.lovepdf.feature.images

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lovepdf.core.pdf.ExportQuality
import com.example.lovepdf.core.pdf.ImageFormat
import com.example.lovepdf.feature.tools.PdfToImagesState
import com.example.lovepdf.ui.components.ToolHeaderAction
import com.example.lovepdf.ui.components.ToolPrimaryButton
import com.example.lovepdf.ui.components.ToolScaffold
import com.example.lovepdf.ui.components.ToolStatusSurface
import com.example.lovepdf.ui.components.WaveRingLoader
import com.example.lovepdf.ui.text.countOf

@Composable
fun PdfToImagesScreen(
    state: PdfToImagesState,
    format: ImageFormat,
    quality: ExportQuality,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onFormatChange: (ImageFormat) -> Unit,
    onQualityChange: (ExportQuality) -> Unit,
    onExport: () -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolScaffold(
        title = "PDF to images",
        onBack = onBack,
        modifier = modifier,
        action = { if (state is PdfToImagesState.Ready) ToolHeaderAction(Icons.Outlined.FolderOpen, "Change file", onPickFile) },
        bottomBar = {
            if (state is PdfToImagesState.Ready) {
                ToolPrimaryButton(
                    text = "Export ${countOf(state.pageCount, "image")}",
                    onClick = onExport,
                )
            }
        },
        overlay = when (state) {
            is PdfToImagesState.Working,
            is PdfToImagesState.Done,
            is PdfToImagesState.Failed -> {
                { ExportStatus(state, onDismissResult) }
            }

            else -> null
        },
    ) {
        when (state) {
            is PdfToImagesState.NoFile -> EmptyPicker(
                message = "Choose the PDF you want to export.",
                onPickFile = onPickFile,
                modifier = Modifier.weight(1f),
            )

            is PdfToImagesState.Loading -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { WaveRingLoader() }

            is PdfToImagesState.Ready -> Column(
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
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp),
                )

                OptionLabel("Format")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "JPG",
                        summary = "Smaller files",
                        selected = format == ImageFormat.Jpeg,
                        onClick = { onFormatChange(ImageFormat.Jpeg) },
                        modifier = Modifier.weight(1f),
                    )
                    OptionCard(
                        title = "PNG",
                        summary = "Lossless",
                        selected = format == ImageFormat.Png,
                        onClick = { onFormatChange(ImageFormat.Png) },
                        modifier = Modifier.weight(1f),
                    )
                }

                OptionLabel("Quality")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "Standard",
                        summary = "For screens",
                        selected = quality == ExportQuality.Standard,
                        onClick = { onQualityChange(ExportQuality.Standard) },
                        modifier = Modifier.weight(1f),
                    )
                    OptionCard(
                        title = "High",
                        summary = "For printing",
                        selected = quality == ExportQuality.High,
                        onClick = { onQualityChange(ExportQuality.High) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "Images are saved to a folder named after the document, " +
                        "so the pages stay together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp),
    )
}

@Composable
private fun OptionCard(
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
internal fun EmptyPicker(
    message: String,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    contentDescription = message,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun ExportStatus(state: PdfToImagesState, onDismiss: () -> Unit) {
    ToolStatusSurface {
        when (state) {
            is PdfToImagesState.Working -> {
                WaveRingLoader()
                Text(
                    text = "Exporting pages",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = "${state.done} of ${state.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            is PdfToImagesState.Done -> {
                Text(
                    text = "Saved ${countOf(state.count, "image")}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "In Downloads/${state.folderLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp)) {
                    Text("Done")
                }
            }

            is PdfToImagesState.Failed -> {
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
