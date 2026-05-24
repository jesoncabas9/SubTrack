package com.example.subtrackai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.text.SimpleDateFormat
import java.util.*

class RenewalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val user = supabase.auth.currentUserOrNull() ?: return Result.success()
        val uid = user.id

        try {
            val subs = supabase.postgrest["subscriptions"]
                .select {
                    filter {
                        eq("user_id", uid)
                    }
                }
                .decodeList<Subscription>()

            val today = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            subs.forEach { sub ->
                val name = sub.name
                val renewalDateStr = sub.renewalDate ?: ""
                
                if (renewalDateStr.isNotBlank()) {
                    try {
                        val renewalDate = sdf.parse(renewalDateStr)
                        if (renewalDate != null) {
                            val diff = renewalDate.time - today.timeInMillis
                            val daysUntil = diff / (1000 * 60 * 60 * 24)

                            if (daysUntil in 0..sub.reminderDays.toLong()) {
                                sendNotification(name, renewalDateStr)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RenewalWorker", "Error parsing date: $renewalDateStr", e)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("RenewalWorker", "Error in doWork", e)
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
