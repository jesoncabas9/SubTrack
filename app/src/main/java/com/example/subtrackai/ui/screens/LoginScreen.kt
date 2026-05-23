package com.example.subtrackai.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.subtrackai.R
import androidx.compose.ui.graphics.Shadow
import com.example.subtrackai.ui.components.FloatingBackgroundObjects
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToCompleteProfile: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val serverClientId = "50643151271-f1889snmi1jp7irq4fpqc8ndok5nnfd8.apps.googleusercontent.com"

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.handleLegacyGoogleSignInResult(account)
        } catch (e: Exception) {
            viewModel.resetState()
            Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle session auto-login
    LaunchedEffect(currentUser) {
        if (currentUser != null && authState is AuthViewModel.AuthState.Idle) {
            viewModel.checkProfileCompletion()
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            is AuthViewModel.AuthState.NeedsProfileCompletion -> {
                onNavigateToCompleteProfile()
                viewModel.resetState()
            }
            is AuthViewModel.AuthState.TriggerLegacySignIn -> {
                // IMPORTANT: Launch intent safely from UI thread effect
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(serverClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
            is AuthViewModel.AuthState.Error -> {
                Toast.makeText(context, (authState as AuthViewModel.AuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
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
        FloatingBackgroundObjects()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.subtrack_bg_login),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                "SubTrack",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 4f),
                        blurRadius = 8f
                    )
                )
            )
            Text(
                "Manage Subscriptions Smarter",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(64.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (authState is AuthViewModel.AuthState.CheckingProfile) "Verifying Profile..." else "Welcome to SubTrack",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (authState is AuthViewModel.AuthState.Loading || 
                        authState is AuthViewModel.AuthState.TriggerLegacySignIn ||
                        authState is AuthViewModel.AuthState.CheckingProfile) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Button(
                            onClick = { viewModel.signInWithGoogle(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepPurple),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(24.dp).background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Secure and private. We never share your data.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
