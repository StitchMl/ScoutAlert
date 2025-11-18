package it.agesci.scoutalert.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.work.OneTimeWorkRequestBuilder

/**
 * Manages:
 * - creation of the notification channel
 * - daily scheduling of the birthday worker
 */
object BirthdayScheduler {

    private const val WORK_NAME = "daily_birthdays_work"

    /**
     * Create the NotificationChannel used by birthday notifications.
     * You can call it from MainActivity.onCreate().
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Use the same CHANNEL_ID defined in BirthdayWorker
            val channel = NotificationChannel(
                BirthdayWorker.CHANNEL_ID,
                "Compleanni unità scout",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche di compleanno per le unità selezionate"
            }

            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule the worker that checks birthdays once a day.
     * It is recreated (keep) if it already exists.
     *
     * Here I have chosen 7:30 a.m. as the default time, but you can change it.
     */
    fun scheduleNext(context: Context) {
        val workRequest =
            PeriodicWorkRequestBuilder<BirthdayWorker>(1, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,   // <--- update even if it already exists
            workRequest
        )
    }

    /**
     * Performs birthday checks once, immediately.
     * Uses the same logic as the daily worker.
     */
    fun triggerNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<BirthdayWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}