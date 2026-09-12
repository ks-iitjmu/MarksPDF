package com.kunalsharma.markspdf.feature.images

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
import com.kunalsharma.markspdf.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.core.pdf.ExportQuality
import com.kunalsharma.markspdf.core.pdf.ImageFormat
import com.kunalsharma.markspdf.feature.tools.PdfToImagesState
import com.kunalsharma.markspdf.ui.components.ToolHeaderAction
import com.kunalsharma.markspdf.ui.components.ToolPrimaryButton
import com.kunalsharma.markspdf.ui.components.ToolScaffold
import com.kunalsharma.markspdf.ui.components.ToolStatusSurface
import com.kunalsharma.markspdf.ui.components.WaveRingLoader

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
        title = stringResource(R.string.export_title),
        onBack = onBack,
        modifier = modifier,
        action = { if (state is PdfToImagesState.Ready) ToolHeaderAction(Icons.Outlined.FolderOpen, stringResource(R.string.cd_change_file), onPickFile) },
        bottomBar = {
            if (state is PdfToImagesState.Ready) {
                ToolPrimaryButton(
                    text = pluralStringResource(R.plurals.export_button, state.pageCount, state.pageCount),
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
                message = stringResource(R.string.export_empty_prompt),
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
                    text = pluralStringResource(R.plurals.page_count, state.pageCount, state.pageCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp),
                )

                OptionLabel(stringResource(R.string.label_format))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = stringResource(R.string.format_jpg),
                        summary = stringResource(R.string.format_jpg_summary),
                        selected = format == ImageFormat.Jpeg,
                        onClick = { onFormatChange(ImageFormat.Jpeg) },
                        modifier = Modifier.weight(1f),
                    )
                    OptionCard(
                        title = stringResource(R.string.format_png),
                        summary = stringResource(R.string.format_png_summary),
                        selected = format == ImageFormat.Png,
                        onClick = { onFormatChange(ImageFormat.Png) },
                        modifier = Modifier.weight(1f),
                    )
                }

                OptionLabel(stringResource(R.string.label_quality))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = stringResource(R.string.quality_standard),
                        summary = stringResource(R.string.quality_standard_summary),
                        selected = quality == ExportQuality.Standard,
                        onClick = { onQualityChange(ExportQuality.Standard) },
                        modifier = Modifier.weight(1f),
                    )
                    OptionCard(
                        title = stringResource(R.string.quality_high),
                        summary = stringResource(R.string.quality_high_summary),
                        selected = quality == ExportQuality.High,
                        onClick = { onQualityChange(ExportQuality.High) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = stringResource(R.string.export_note),
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
                    text = stringResource(R.string.export_progress),
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
                    text = pluralStringResource(R.plurals.export_saved, state.count, state.count),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.in_downloads, state.folderLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp)) {
                    Text(stringResource(R.string.action_done))
                }
            }

            is PdfToImagesState.Failed -> {
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
