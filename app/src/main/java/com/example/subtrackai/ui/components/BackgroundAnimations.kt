package com.example.subtrackai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FloatingBackgroundObjects() {
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingObject(icon = Icons.Default.CreditCard, xOffset = 40.dp, yOffset = 100.dp, delay = 0)
        FloatingObject(icon = Icons.Default.MonetizationOn, xOffset = 300.dp, yOffset = 150.dp, delay = 1000)
        FloatingObject(icon = Icons.Default.Receipt, xOffset = 60.dp, yOffset = 600.dp, delay = 500)
        FloatingObject(icon = Icons.Default.Star, xOffset = 250.dp, yOffset = 550.dp, delay = 1500)
        FloatingObject(icon = Icons.Default.CreditCard, xOffset = 320.dp, yOffset = 700.dp, delay = 2000)
    }
}

@Composable
fun FloatingObject(icon: ImageVector, xOffset: Dp, yOffset: Dp, delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    
    val dy by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 20.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .offset(x = xOffset, y = yOffset + dy)
            .rotate(rotation)
            .size(48.dp)
            .alpha(0.15f),
        tint = Color.White
    )
}

@Composable
fun JumpingDotsLoading(color: Color = Color.White) {
    val dots = 4
    val dotSize = 8.dp
    val jumpHeight = 10.dp
    
    // Total steps in the sequence: 1 -> 2 -> 3 -> 4 -> 3 -> 2 -> (total 6 steps per cycle)
    // We use a single transition to drive the active dot index
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    // Each jump takes 300ms, total cycle 1800ms
    val stepDuration = 300
    val totalSteps = 6 // 0, 1, 2, 3, 4, 5 corresponding to indices [0, 1, 2, 3, 2, 1]
    
    val currentStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalSteps.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(stepDuration * totalSteps, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "currentStep"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        // Map current step to active dot index
        // 0->0, 1->1, 2->2, 3->3, 4->2, 5->1
        val activeDotIndex = when (currentStep.toInt()) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 2
            5 -> 1
            else -> 0
        }
        
        // Progress within the current step (0.0 to 1.0)
        val stepProgress = currentStep % 1f
        
        // Smooth jump: sine wave from 0 to PI
        val verticalOffset = if (stepProgress < 1f) {
            -jumpHeight * kotlin.math.sin(stepProgress * Math.PI).toFloat()
        } else 0.dp

        for (i in 0 until dots) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = if (i == activeDotIndex) verticalOffset else 0.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
