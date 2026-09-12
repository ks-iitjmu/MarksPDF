package com.kunalsharma.markspdf.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kunalsharma.markspdf.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import com.kunalsharma.markspdf.core.settings.AppLanguage

@Composable
fun LanguageDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    LanguageRow(
                        language = language,
                        selected = language == selected,
                        onClick = { onSelect(language) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
    )
}
