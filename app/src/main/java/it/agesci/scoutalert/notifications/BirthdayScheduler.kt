package it.agesci.scoutalert.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Manages:
 * - creation of the notification channel
 * - daily scheduling of the birthday worker
 */
object BirthdayScheduler {

    private const val WORK_NAME = "daily_birthdays_work"
    private const val IMMEDIATE_WORK_NAME = "immediate_birthdays_work"

    /**
     * Create the NotificationChannel used by birthday notifications.
     * You can call it from MainActivity.onCreate().
     */
    fun createNotificationChannel(context: Context) {
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

    /**
     * Schedule the worker that checks birthdays once a day.
     * It is recreated (keep) if it already exists.
     *
     * Here I have chosen 7:30 a.m. as the default time, but you can change it.
     */
    fun scheduleNext(context: Context) {
        enqueueNext(context, ExistingWorkPolicy.REPLACE)
    }

    internal fun scheduleNextAfterRun(context: Context) {
        enqueueNext(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueueNext(context: Context, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now()
        val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
            .setInitialDelay(delayUntilNextBirthdayCheck(now))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            policy,
            workRequest
        )
    }

    /**
     * Performs birthday checks once, immediately.
     * Uses the same logic as the daily worker.
     */
    fun triggerNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<BirthdayWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
