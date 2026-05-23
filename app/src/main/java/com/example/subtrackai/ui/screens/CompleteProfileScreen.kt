package com.example.subtrackai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.util.ProfileIcons
import com.example.subtrackai.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun CompleteProfileScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val user = remember { supabase.auth.currentUserOrNull() }
    val googleName = remember { user?.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\"") ?: "" }
    val googleAvatar = remember { user?.userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\"") }

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf(googleName) }
    var selectedIcon by remember { mutableStateOf("Google") } // Default to Google
    var selectedCurrency by remember { mutableStateOf("$") }
    
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val currencies = listOf("$", "€", "£", "¥", "₹")

    // Debounced username check
    LaunchedEffect(username) {
        if (username.length >= 3) {
            isCheckingUsername = true
            delay(500)
            usernameAvailable = viewModel.checkUsernameAvailable(username)
            isCheckingUsername = false
        } else {
            usernameAvailable = null
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            onComplete()
        } else if (authState is AuthViewModel.AuthState.Error) {
            Toast.makeText(context, (authState as AuthViewModel.AuthState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepPurple, DeepBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Final Step",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "Choose your unique username to start",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Avatar Picker
            Text("Choose an Avatar", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Google Avatar Option
                item {
                    val isSelected = selectedIcon == "Google"
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.1f))
                            .border(3.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                            .clickable { selectedIcon = "Google" },
                        contentAlignment = Alignment.Center
                    ) {
                        if (googleAvatar != null) {
                            AsyncImage(
                                model = googleAvatar,
                                contentDescription = "Google Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person, 
                                contentDescription = null, 
                                tint = if (isSelected) DeepPurple else Color.White
                            )
                        }
                    }
                }

                // Local Icons
                items(ProfileIcons.icons.toList().size) { index ->
                    val (name, icon) = ProfileIcons.icons.toList()[index]
                    val isSelected = selectedIcon == name
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.1f))
                            .border(3.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                            .clickable { selectedIcon = name },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) DeepPurple else Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Your Name", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { if (it.length <= 15) username = it.lowercase().filter { c -> c.isLetterOrDigit() } },
                        label = { Text("Unique Username", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isCheckingUsername) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else if (usernameAvailable == true) {
                                Icon(Icons.Default.Check, contentDescription = "Available", tint = Color.Green)
                            } else if (usernameAvailable == false) {
                                Icon(Icons.Default.Close, contentDescription = "Taken", tint = Color.Red)
                            }
                        },
                        supportingText = {
                            if (usernameAvailable == false) {
                                Text("This username is already taken", color = Color.Red)
                            } else {
                                Text("No spaces or special characters", color = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (usernameAvailable == false) Color.Red else Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Preferred Currency", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencies.forEach { curr ->
                            val isSelected = selectedCurrency == curr
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.1f))
                                    .clickable { selectedCurrency = curr },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(curr, color = if (isSelected) DeepPurple else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.completeProfile(username, fullName, selectedIcon, selectedCurrency) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepPurple),
                        enabled = usernameAvailable == true && fullName.isNotBlank() && authState !is AuthViewModel.AuthState.Loading
                    ) {
                        if (authState is AuthViewModel.AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DeepPurple)
                        } else {
                            Text("Start Tracking", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
