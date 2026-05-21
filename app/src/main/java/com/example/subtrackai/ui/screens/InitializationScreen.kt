package com.example.subtrackai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.subtrackai.R
import com.example.subtrackai.ui.components.JumpingDotsLoading
import com.example.subtrackai.ui.theme.DeepPurple
import kotlinx.coroutines.delay

@Composable
fun InitializationScreen(onInitializationComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // Wait for 2 seconds
        onInitializationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Logo perfectly centered
        Surface(
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.subtrack_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
        }
        
        // Dots at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            JumpingDotsLoading(color = DeepPurple)
        }
    }
}
