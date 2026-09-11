package com.example.lovepdf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class FabMenuAction(
    val icon: ImageVector,
    val label: String,
    val active: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * A floating action button that opens into a stack of labelled actions.
 *
 * Each action carries a written label, which is the whole reason to prefer this over a
 * row of bare icon buttons: an icon alone is a guess until you've tapped it once, and a
 * PDF toolbar is full of icons that don't have settled conventions.
 *
 * Items animate in bottom-up with a stagger, so the eye is led from the button that was
 * just pressed towards the choices, rather than having a block appear all at once.
 */
@Composable
fun ToolsFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: List<FabMenuAction>,
    modifier: Modifier = Modifier,
) {
    val fabRotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEachIndexed { index, action ->
            // Closest item animates first on the way in and last on the way out, so the
            // stack unfolds and folds from the button rather than from the far end.
            val position = actions.lastIndex - index
            val enterDelay = position * 35
            val exitDelay = index * 25

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180, delayMillis = enterDelay)) +
                    slideInVertically(
                        animationSpec = tween(220, delayMillis = enterDelay),
                        initialOffsetY = { it / 2 },
                    ) +
                    scaleIn(tween(220, delayMillis = enterDelay), initialScale = 0.85f),
                exit = fadeOut(tween(120, delayMillis = exitDelay)) +
                    slideOutVertically(
                        animationSpec = tween(160, delayMillis = exitDelay),
                        targetOffsetY = { it / 2 },
                    ) +
                    scaleOut(tween(160, delayMillis = exitDelay), targetScale = 0.85f),
            ) {
                MenuItem(action = action, onDismiss = { onExpandedChange(false) })
            }
        }

        Surface(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    // One icon rotated rather than two swapped: the turn carries the
                    // open/close state without the glyph jumping.
                    imageVector = if (expanded) Icons.Outlined.Close else Icons.Outlined.Tune,
                    contentDescription = if (expanded) "Close tools" else "Tools",
                    modifier = Modifier.graphicsLayer { rotationZ = fabRotation },
                )
            }
        }
    }
}

/** Full-screen catcher so a tap anywhere outside the menu closes it. */
@Composable
fun FabMenuScrim(expanded: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onDismiss,
                )
        )
    }
}

@Composable
private fun MenuItem(action: FabMenuAction, onDismiss: () -> Unit) {
    Surface(
        onClick = {
            action.onClick()
            onDismiss()
        },
        shape = RoundedCornerShape(percent = 50),
        color = if (action.active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (action.active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(text = action.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
