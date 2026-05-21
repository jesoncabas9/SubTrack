package com.example.subtrackai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Preferences for "Keep me signed in"
    private var sharedPrefsName = "subtrack_prefs"

    fun login(email: String, pass: String, keepSignedIn: Boolean, context: Context) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user != null && !user.isEmailVerified) {
                    _authState.value = AuthState.Error("Please verify your email address.")
                    auth.signOut()
                    return@addOnSuccessListener
                }
                
                if (keepSignedIn) {
                    val prefs = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("keep_signed_in", true).apply()
                }
                // Update user state AFTER prefs are set
                _currentUser.value = auth.currentUser
                _authState.value = AuthState.Success("Login Successful")
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Login Failed")
            }
    }

    fun signUp(email: String, pass: String, fullName: String, username: String, profileIcon: String, currency: String = "$") {
        _authState.value = AuthState.SignUpLoading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val user = result.user
                val uid = user?.uid ?: return@addOnSuccessListener
                
                val userData = UserProfile(
                    uid = uid,
                    fullName = fullName,
                    username = username,
                    email = email,
                    profileIcon = profileIcon,
                    bio = "New to SubTrack!",
                    showSubscriptions = true,
                    friendsCount = 0,
                    currency = currency
                )
                
                // Update Firebase User Profile
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = username
                }
                user.updateProfile(profileUpdates)

                // Save additional user data to Firestore
                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                auth.signOut()
                                _currentUser.value = null
                                _authState.value = AuthState.SignUpSuccess
                            }
                            .addOnFailureListener {
                                _authState.value = AuthState.Error("Failed to send verification email: ${it.message}")
                            }
                    }
                    .addOnFailureListener {
                        _authState.value = AuthState.Error("Sign up worked but failed to save profile: ${it.message}")
                    }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Sign Up Failed")
            }
    }

    fun signOut(context: Context) {
        _authState.value = AuthState.Loading // 1. Set loading immediately
        
        // Use viewModelScope to ensure we have a coroutine context
        viewModelScope.launch {
            delay(800) // Ensure the loading screen is visible
            auth.signOut()
            _currentUser.value = null
            val prefs = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("keep_signed_in", false).apply()
            _authState.value = AuthState.Idle
        }
    }

    fun nukeUsersCollection() {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                // Delete user's sub-collections first if needed, but for a simple nuke this is okay
                doc.reference.delete()
            }
        }
        firestore.collection("friendRequests").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
        firestore.collection("posts").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object SignUpLoading : AuthState()
        data class Success(val message: String) : AuthState()
        object SignUpSuccess : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
