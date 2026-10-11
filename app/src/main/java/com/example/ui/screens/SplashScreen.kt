package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isDarkMode: Boolean = true,
    onTimeout: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = twistAnimationSpec(1500),
        label = "Alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500) // 2.5 seconds splash experience
        onTimeout()
    }

    val backgroundBrush = if (isDarkMode) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E1B4B), // Dark indigo center
                Color(0xFF0F0D22), // Outer space darkness
                Color(0xFF070412)
            ),
            radius = 1200f
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEFF6FF), // Soft azure light list
                Color(0xFFF8FAFC)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // Premium Anha.ai Logo Icon
            Image(
                painter = painterResource(id = R.drawable.img_anha_logo),
                contentDescription = "Anha.ai Brand Emblem",
                modifier = Modifier
                    .size(112.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Anha.ai",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color(0xFF1E3A8A),
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Breaking Language Barriers Instantly",
                fontSize = 15.sp,
                color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF475569),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your Intelligent Language Companion",
                fontSize = 13.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF8B5CF6),
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun <T> twistAnimationSpec(duration: Int): AnimationSpec<T> {
    return tween(durationMillis = duration, easing = FastOutSlowInEasing)
}
