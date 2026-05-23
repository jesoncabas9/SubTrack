package com.example.subtrackai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.viewmodel.AuthViewModel
import com.example.subtrackai.util.ProfileIcons
import com.example.subtrackai.viewmodel.SettingsViewModel
import com.example.subtrackai.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    socialViewModel: SocialViewModel,
    settingsViewModel: SettingsViewModel,
    dashboardViewModel: com.example.subtrackai.viewmodel.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val currentCurrency by settingsViewModel.selectedCurrency.collectAsState()
    val context = LocalContext.current
    val userProfile by socialViewModel.userProfile.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Status Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.subtrackai.ui.components.ProfileAvatar(
                        iconName = userProfile?.profileIcon,
                        avatarUrl = userProfile?.avatarUrl,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile?.fullName ?: "User", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Status: ${userProfile?.userStatus ?: "Online"}", fontSize = 14.sp, color = Color.Gray)
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change Status")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("Online", "Do Not Disturb", "Asleep").forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        socialViewModel.updateUserStatus(status)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Text("Preferences", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            SettingsItem(
                icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = if (isDarkMode) "Enabled" else "Disabled",
                trailing = {
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }
            )

            var showCurrencyDialog by remember { mutableStateOf(false) }
            SettingsItem(
                icon = Icons.Default.MonetizationOn,
                title = "Currency",
                subtitle = "Active: $currentCurrency",
                onClick = { showCurrencyDialog = true }
            )

            if (showCurrencyDialog) {
                AlertDialog(
                    onDismissRequest = { showCurrencyDialog = false },
                    title = { Text("Select Currency") },
                    text = {
                        Column {
                            settingsViewModel.currencies.forEach { currency ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            val oldCurrency = currentCurrency
                                            settingsViewModel.setCurrency(currency)
                                            socialViewModel.updateCurrency(currency)
                                            
                                            if (oldCurrency != currency) {
                                                dashboardViewModel.updateCurrencyPrices(oldCurrency, currency, settingsViewModel)
                                            }

                                            showCurrencyDialog = false
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = currentCurrency == currency, onClick = null)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(currency)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
                    }
                )
            }

            val showInsights by settingsViewModel.showSmartInsights.collectAsState()
            SettingsItem(
                icon = Icons.Default.Lightbulb,
                title = "Smart Insights",
                subtitle = if (showInsights) "Enabled" else "Disabled",
                trailing = {
                    Switch(checked = showInsights, onCheckedChange = { settingsViewModel.setShowSmartInsights(it) })
                }
            )

            Text("Account", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = { authViewModel.signOut(context) },
                titleColor = Color.Red
            )
        }
    }
}

@Composable
fun StatusChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleColor: Color = Color.Unspecified
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (titleColor == Color.Red) Color.Red else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = titleColor)
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
            }
            trailing?.invoke()
        }
    }
}
