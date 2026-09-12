package com.example.lovepdf.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lovepdf.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_love_pdf_logo),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
