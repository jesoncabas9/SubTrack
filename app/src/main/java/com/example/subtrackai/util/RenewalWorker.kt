package com.example.subtrackai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subtrackai.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RenewalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success()

        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("subscriptions")
                .get()
                .await()

            val today = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            snapshot.documents.forEach { doc ->
                val name = doc.getString("name") ?: ""
                val renewalDateStr = doc.getString("renewalDate") ?: ""
                
                if (renewalDateStr.isNotBlank()) {
                    val renewalDate = sdf.parse(renewalDateStr)
                    if (renewalDate != null) {
                        val diff = renewalDate.time - today.timeInMillis
                        val daysUntil = diff / (1000 * 60 * 60 * 24)

                        if (daysUntil in 0..2) {
                            sendNotification(name, renewalDateStr)
                        }
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(subName: String, date: String) {
        val channelId = "renewal_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Renewal Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Subscription Renewal")
            .setContentText("Your $subName subscription renews on $date!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(subName.hashCode(), notification)
    }
}
