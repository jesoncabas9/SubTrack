package com.example.subtrackai.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.UserProfile
import com.example.subtrackai.supabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel : ViewModel() {
    
    val currentUser: StateFlow<UserInfo?> = supabase.auth.sessionStatus.map { status ->
        if (status is SessionStatus.Authenticated) status.session.user else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), supabase.auth.currentUserOrNull())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // New state to track if the profile is complete (has username)
    private val _isProfileComplete = MutableStateFlow<Boolean?>(null)
    val isProfileComplete: StateFlow<Boolean?> = _isProfileComplete.asStateFlow()

    private val serverClientId = "50643151271-f1889snmi1jp7irq4fpqc8ndok5nnfd8.apps.googleusercontent.com"

    init {
        // Automatically check profile whenever user signs in or app starts with session
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    checkProfileCompletion()
                } else {
                    _isProfileComplete.value = null
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _authState.value = AuthState.Loading
        val credentialManager = CredentialManager.create(context)
        
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = hashNonce(rawNonce)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                handleGoogleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.w("AuthViewModel", "Modern API failed: ${e.message}")
                _authState.value = AuthState.TriggerLegacySignIn
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign-In Error", e)
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Failed")
            }
        }
    }

    private fun hashNonce(nonce: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(nonce.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            signInToSupabaseWithIdToken(credential.idToken)
        } else {
            _authState.value = AuthState.Error("Unexpected credential type")
        }
    }

    fun handleLegacyGoogleSignInResult(account: GoogleSignInAccount?) {
        val idToken = account?.idToken
        if (idToken != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                signInToSupabaseWithIdToken(idToken)
            }
        } else {
            _authState.value = AuthState.Error("Failed to get ID Token from Google")
        }
    }

    private suspend fun signInToSupabaseWithIdToken(idTokenStr: String) {
        try {
            supabase.auth.signInWith(IDToken) {
                idToken = idTokenStr
                provider = Google
            }
            // Transition happens via the observer in init {}
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Supabase Auth Failed")
        }
    }

    fun checkProfileCompletion() {
        viewModelScope.launch {
            try {
                val user = supabase.auth.currentUserOrNull() ?: return@launch
                
                val profile = supabase.postgrest["profiles"]
                    .select {
                        filter { eq("id", user.id) }
                    }
                    .decodeSingleOrNull<UserProfile>()

                if (profile != null && !profile.username.isNullOrBlank()) {
                    _isProfileComplete.value = true
                    _authState.value = AuthState.Success("Welcome back!")
                } else {
                    _isProfileComplete.value = false
                    _authState.value = AuthState.NeedsProfileCompletion
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile check failed", e)
                _isProfileComplete.value = false
                _authState.value = AuthState.NeedsProfileCompletion
            }
        }
    }

    fun completeProfile(username: String, fullName: String, profileIcon: String, currency: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // Get Google avatar if available
                val googleAvatar = user.userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\"")
                
                val userData = UserProfile(
                    uid = user.id,
                    fullName = fullName,
                    username = username,
                    email = user.email ?: "",
                    profileIcon = profileIcon,
                    avatarUrl = googleAvatar,
                    currency = currency
                )
                supabase.postgrest["profiles"].upsert(userData)
                _isProfileComplete.value = true
                _authState.value = AuthState.Success("Profile Setup Complete!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to save profile")
            }
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return try {
            val exists = supabase.postgrest.rpc("check_username_exists", buildJsonObject {
                put("username_to_check", username)
            }).decodeAs<Boolean>()
            !exists
        } catch (e: Exception) {
            false
        }
    }

    fun signOut(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                delay(800)
                supabase.auth.signOut()
                _isProfileComplete.value = null
                val prefs = context.getSharedPreferences("subtrack_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("keep_signed_in", false).apply()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object CheckingProfile : AuthState()
        object TriggerLegacySignIn : AuthState()
        object NeedsProfileCompletion : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
