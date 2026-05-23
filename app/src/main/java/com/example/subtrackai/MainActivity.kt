package com.example.subtrackai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.subtrackai.navigation.AppNavigation
import com.example.subtrackai.util.RenewalWorker
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

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
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            val uid = user.id
            lifecycleScope.launch {
                try {
                    supabase.postgrest["profiles"].update(
                        mapOf(
                            "is_online" to true,
                            "user_status" to "Online"
                        )
                    ) {
                        filter {
                            eq("id", uid)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating online status", e)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            val uid = user.id
            lifecycleScope.launch {
                try {
                    supabase.postgrest["profiles"].update(
                        mapOf(
                            "is_online" to false,
                            "user_status" to "Offline"
                        )
                    ) {
                        filter {
                            eq("id", uid)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating offline status", e)
                }
            }
        }
    }
}
