package com.example.cgallery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun StartupAnimation(
    onAnimationComplete: () -> Unit
) {
    var animationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200.milliseconds) // Hold for 1.2s
        animationFinished = true
        onAnimationComplete()
    }

    if (!animationFinished) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141218)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "cGallery v0.72",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )
            )
        }
    }
}
