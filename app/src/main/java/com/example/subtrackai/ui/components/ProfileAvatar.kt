package com.example.subtrackai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.subtrackai.util.ProfileIcons

@Composable
fun ProfileAvatar(
    iconName: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    userName: String? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (iconName == "Google" && avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (iconName == "Google" && userName != null) {
            Text(
                text = userName.take(1).uppercase(),
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        } else {
            Icon(
                imageVector = ProfileIcons.getIcon(iconName),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}
