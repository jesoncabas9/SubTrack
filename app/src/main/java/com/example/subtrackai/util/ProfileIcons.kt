package com.example.subtrackai.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object ProfileIcons {
    val icons = mapOf(
        "Person" to Icons.Default.Person,
        "Rocket" to Icons.Default.RocketLaunch,
        "Star" to Icons.Default.Star,
        "Face" to Icons.Default.Face,
        "Pets" to Icons.Default.Pets,
        "Eco" to Icons.Default.Eco,
        "Coffee" to Icons.Default.Coffee,
        "Gamepad" to Icons.Default.Gamepad,
        "School" to Icons.Default.School,
        "Fitness" to Icons.Default.FitnessCenter,
        "Music" to Icons.Default.MusicNote,
        "Camera" to Icons.Default.CameraAlt
    )

    fun getIcon(name: String?): ImageVector {
        return icons[name] ?: Icons.Default.Person
    }
}
