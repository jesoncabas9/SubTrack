package com.example.subtrackai

import android.app.Application
import com.google.firebase.FirebaseApp

class SubTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase. 
        // Note: This will still fail if google-services.json is missing 
        // UNLESS the user provides explicit FirebaseOptions.
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
