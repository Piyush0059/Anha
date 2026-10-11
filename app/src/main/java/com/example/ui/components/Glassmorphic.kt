package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isDarkTheme: Boolean = true,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val bgBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x12FFFFFF), // white/7% transparency
                Color(0x05FFFFFF)  // white/2% transparency
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.95f),
                Color(0xFFF1F5F9).copy(alpha = 0.85f)
            )
        )
    }

    val borderBrush = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF9333EA).copy(alpha = 0.25f), // Purple-600 subtle glow
                Color(0xFF3B82F6).copy(alpha = 0.10f)  // Blue-500 leakage
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFCBD5E1).copy(alpha = 0.7f),
                Color(0xFFF1F5F9).copy(alpha = 0.4f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = bgBrush)
            .padding(16.dp)
    ) {
        content()
    }
}
