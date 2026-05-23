package com.example.subtrackai

import android.app.Application
import com.google.firebase.FirebaseApp

class SubTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase (if still needed for other services)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Supabase is initialized as a singleton in SupabaseModule.kt
    }
}
