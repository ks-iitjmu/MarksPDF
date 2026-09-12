package com.example.lovepdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

enum class ToolAccent { Violet, Rose, Mint, Sky, Amber, Lilac, Peach, Teal }


data class ToolCardColors(
    val container: Color,
    val onContainer: Color,
    val support: Color,
    val chip: Color,
)

@Composable
fun toolCardColors(accent: ToolAccent): ToolCardColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = if (dark) accent.darkContainer else accent.lightContainer
    val onContainer = if (dark) accent.darkLabel else accent.lightLabel

    return ToolCardColors(
        container = container,
        onContainer = onContainer,
        support = onContainer.copy(alpha = 0.75f),
        chip = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
    )
}

private val ToolAccent.lightContainer: Color
    get() = when (this) {
        ToolAccent.Violet -> Color(0xFFEDE7FB)
        ToolAccent.Rose -> Color(0xFFFBE4E2)
        ToolAccent.Mint -> Color(0xFFD9F2E3)
        ToolAccent.Sky -> Color(0xFFDDEBFA)
        ToolAccent.Amber -> Color(0xFFFAF0CE)
        ToolAccent.Lilac -> Color(0xFFE7E1FA)
        ToolAccent.Peach -> Color(0xFFFBE3D3)
        ToolAccent.Teal -> Color(0xFFD7EFEE)
    }

private val ToolAccent.lightLabel: Color
    get() = when (this) {
        ToolAccent.Violet -> Color(0xFF5B3FBF)
        ToolAccent.Rose -> Color(0xFFC0392B)
        ToolAccent.Mint -> Color(0xFF1E7A4D)
        ToolAccent.Sky -> Color(0xFF2563A8)
        ToolAccent.Amber -> Color(0xFF8A6000)
        ToolAccent.Lilac -> Color(0xFF5A4BC4)
        ToolAccent.Peach -> Color(0xFFB3541E)
        ToolAccent.Teal -> Color(0xFF16706B)
    }

private val ToolAccent.darkContainer: Color
    get() = when (this) {
        ToolAccent.Violet -> Color(0xFF2E2545)
        ToolAccent.Rose -> Color(0xFF412826)
        ToolAccent.Mint -> Color(0xFF1F3A2D)
        ToolAccent.Sky -> Color(0xFF1F2E40)
        ToolAccent.Amber -> Color(0xFF3D3520)
        ToolAccent.Lilac -> Color(0xFF2A2740)
        ToolAccent.Peach -> Color(0xFF40301F)
        ToolAccent.Teal -> Color(0xFF1D3736)
    }

private val ToolAccent.darkLabel: Color
    get() = when (this) {
        ToolAccent.Violet -> Color(0xFFCDBDFF)
        ToolAccent.Rose -> Color(0xFFFFB4AB)
        ToolAccent.Mint -> Color(0xFFA8E6C3)
        ToolAccent.Sky -> Color(0xFFA9CFF5)
        ToolAccent.Amber -> Color(0xFFF2D488)
        ToolAccent.Lilac -> Color(0xFFC6BFF5)
        ToolAccent.Peach -> Color(0xFFF5C4A0)
        ToolAccent.Teal -> Color(0xFFA5DCD8)
    }
