package com.example.lovepdf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.lovepdf.ui.theme.Motion

@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    overlay: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PillHeader(
                title = title,
                leading = {
                    PillIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        label = "Back",
                        onClick = onBack,
                    )
                },
                trailing = { action?.invoke() ?: PillButtonSpacer() },
            )

            content()

            bottomBar?.invoke()
        }

        if (overlay != null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(Motion.enter()),
                exit = fadeOut(Motion.exit()),
            ) {
                overlay()
            }
        }
    }
}

@Composable
fun ToolPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun ToolHeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    PillIconButton(icon = icon, label = label, onClick = onClick)
}
@Composable
fun ToolStatusSurface(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
            content = content,
        )
    }
}
