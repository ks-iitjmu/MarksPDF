package com.example.lovepdf.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

object Motion {

    const val Quick = 140
    const val Standard = 200

    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(Standard, easing = Enter)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(Quick, easing = Exit)

    val slideSpec: FiniteAnimationSpec<IntOffset> = tween(Standard, easing = Enter)
}
