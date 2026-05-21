package com.example.subtrackai

import android.app.Application
import com.google.firebase.FirebaseApp

class SubTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            
            // Enable Firestore Offline Persistence for better performance
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100MB cache
                    .build())
                .build()
            firestore.firestoreSettings = settings

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
