package com.example.lovepdf.feature.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Search control that starts as a button and grows into a search bar.
 *
 * Collapsed it's the same size and shape as the tools button on the other side, so the
 * two read as a pair. Expanded it holds the query, the match position, and the two
 * steppers — everything needed to work through results without leaving the page.
 *
 * Results are reported per page rather than per occurrence. The viewer renders pages as
 * bitmaps and has no word positions to highlight or scroll to, so claiming
 * occurrence-level precision in the counter would promise navigation the reader can't
 * actually perform.
 */
@Composable
fun SearchFab(
    expanded: Boolean,
    state: SearchState,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = if (expanded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        contentColor = if (expanded) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        shadowElevation = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!expanded) {
                IconButton(
                    onClick = { onExpandedChange(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search in document")
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )

                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                        if (state.query.isEmpty()) {
                            Text(
                                text = "Find",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                    .copy(alpha = 0.6f),
                            )
                        }
                    }

                    StatusLabel(state)

                    IconButton(
                        onClick = { onStep(-1) },
                        enabled = state.hasMatches,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Previous match",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { onStep(1) },
                        enabled = state.hasMatches,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Next match",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { onExpandedChange(false) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close search",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tells the reader what the steppers will do before they press them.
 *
 * The first search on a long document takes real time, so the indexing percentage is
 * shown rather than leaving the field looking unresponsive.
 */
@Composable
private fun StatusLabel(state: SearchState) {
    val text = when {
        state.indexing -> "${(state.indexProgress * 100).toInt()}%"
        state.hasMatches -> "${state.current + 1}/${state.matches.size}"
        state.searched -> "0"
        else -> ""
    }

    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}
