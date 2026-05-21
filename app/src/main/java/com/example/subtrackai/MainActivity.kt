package com.example.subtrackai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.subtrackai.navigation.AppNavigation
import com.example.subtrackai.util.RenewalWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val authViewModel = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Schedule Renewal Notifications
        val workRequest = PeriodicWorkRequestBuilder<RenewalWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "renewal_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            AppNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = authViewModel.currentUser?.uid
        if (uid != null) {
            val updates = mapOf(
                "isOnline" to true,
                "userStatus" to "Online"
            )
            firestore.collection("users").document(uid).update(updates)
        }
    }

    override fun onStop() {
        super.onStop()
        val uid = authViewModel.currentUser?.uid
        if (uid != null) {
            val updates = mapOf(
                "isOnline" to false,
                "userStatus" to "Offline"
            )
            firestore.collection("users").document(uid).update(updates)
        }
    }
}
