package com.kunalsharma.markspdf.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kunalsharma.markspdf.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.core.settings.AppLanguage
import com.kunalsharma.markspdf.ui.components.AppLogo
import com.kunalsharma.markspdf.ui.components.dottedBackground
import kotlinx.coroutines.launch

private data class IntroPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

@Composable
fun IntroScreen(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = listOf(
        IntroPage(
            icon = Icons.AutoMirrored.Outlined.MergeType,
            title = stringResource(R.string.intro_1_title),
            body = stringResource(R.string.intro_1_body),
        ),
        IntroPage(
            icon = Icons.Outlined.PhotoLibrary,
            title = stringResource(R.string.intro_2_title),
            body = stringResource(R.string.intro_2_body),
        ),
        IntroPage(
            icon = Icons.Outlined.Image,
            title = stringResource(R.string.intro_3_title),
            body = stringResource(R.string.intro_3_body),
        ),
        IntroPage(
            icon = Icons.Outlined.ContentCut,
            title = stringResource(R.string.intro_4_title),
            body = stringResource(R.string.intro_4_body),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size + 1 })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .dottedBackground()
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppLogo(size = 24.dp)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                if (!isLastPage) {
                    TextButton(onClick = { scope.launch { pagerState.scrollToPage(pages.size) } }) {
                        Text(stringResource(R.string.action_skip))
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                if (index < pages.size) {
                    FeaturePage(pages[index])
                } else {
                    LanguagePage(
                        selected = selectedLanguage,
                        onSelect = onLanguageChange,
                    )
                }
            }

            PageDots(
                count = pages.size + 1,
                current = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = stringResource(if (isLastPage) R.string.intro_start else R.string.action_next),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePage(page: IntroPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(104.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun LanguagePage(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Text(
            text = stringResource(R.string.intro_language_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
        )

        AppLanguage.entries.forEach { language ->
            LanguageRow(
                language = language,
                selected = language == selected,
                onClick = { onSelect(language) },
            )
        }
    }
}

@Composable
fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = language.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = language.englishName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            // The current dot stretches into a bar rather than just changing colour, so
            // position is readable at a glance without counting.
            val width by animateDpAsState(
                targetValue = if (index == current) 22.dp else 6.dp,
                label = "dotWidth",
            )
            val alpha by animateFloatAsState(
                targetValue = if (index == current) 1f else 0.3f,
                label = "dotAlpha",
            )
            Box(
                Modifier
                    .width(width)
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        CircleShape,
                    )
            )
        }
    }
}
