package com.example.cgallery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun StartupAnimation(
    onAnimationComplete: () -> Unit
) {
    val words = listOf(
        "crafted", "custom", "centralised",
        "catalogued", "controlled", "curated",
        "collected", "connected", "cherished",
        "cGallery"
    )

    var visibleCount by remember { mutableIntStateOf(0) }
    var animationFinished by remember { mutableStateOf(false) }

    // Fast, uniform list formation
    LaunchedEffect(Unit) {
        for (i in 1..words.size) {
            visibleCount = i
            delay(100.milliseconds)
        }
        delay(800.milliseconds) // Brief hold
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
            val lineHeight = 36.dp
            
            // This box acts as the anchor. Its center is the screen center.
            // All words are positioned relative to this center point.
            Box(modifier = Modifier.wrapContentSize()) {
                // Anchor: Invisible text to define the fixed horizontal start for all 'c's
                Text(
                    text = "cGallery v0.69",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    modifier = Modifier.alpha(0f)
                )

                // Reveal the list items
                for (i in 0 until visibleCount) {
                    val word = words[i]
                    val isLast = i == words.lastIndex
                    // Position items so the final brand (cGallery) is at the center
                    val yOffset = lineHeight * (i - (words.size - 1))
                    
                    Row(
                        modifier = Modifier
                            .offset(y = yOffset)
                            .height(lineHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "c",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = word.substring(1) + (if (isLast) " v0.69" else ""),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 26.sp,
                                color = if (isLast) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }
    }
}
