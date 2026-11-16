package it.agesci.scoutalert.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.agesci.scoutalert.MainActivity
import it.agesci.scoutalert.R
import it.agesci.scoutalert.widget.BirthdayWidgetProvider
import org.json.JSONArray
import org.json.JSONException
import java.time.LocalDate

class BirthdayWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private var shouldScheduleNextRun = true

    companion object {
        const val CHANNEL_ID = "birthdays_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "BirthdayWorker"
        private const val PREFS_NAME = "birthdays_prefs"
        private const val BIRTHDAYS_KEY = "birthdays_json"
        private const val UNITS_KEY = "units_with_notifications"
        private const val LAST_NOTIFICATION_DATE_KEY = "last_notification_date"
        private const val LAST_NOTIFICATION_FINGERPRINT_KEY = "last_notification_fingerprint"
        private const val MAX_DELIVERY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        return try {
            runBirthdayCheck()
        } finally {
            if (shouldScheduleNextRun) {
                BirthdayScheduler.scheduleNextAfterRun(applicationContext)
            }
        }
    }

    private fun runBirthdayCheck(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(BIRTHDAYS_KEY, null)

        BirthdayWidgetProvider.updateAllWidgets(context)
        if (json.isNullOrBlank()) return Result.success()

        val unitsWithNotifications =
            prefs.getStringSet(UNITS_KEY, emptySet()) ?: emptySet()

        val today = LocalDate.now()
        val birthdaysToday = mutableListOf<BirthdayInfo>()

        val arr = try {
            JSONArray(json)
        } catch (error: JSONException) {
            Log.e(TAG, "Stored birthday data is invalid", error)
            return Result.failure()
        }

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val day = o.optInt("day", -1)
            val month = o.optInt("month", -1)
            val unit = o.optString("unit").trim().ifEmpty { null }

            if (matchesBirthday(today, day, month, unit, unitsWithNotifications)) {
                birthdaysToday += BirthdayInfo(
                    firstName = o.optString("firstName"),
                    lastName = o.optString("lastName"),
                    unit = unit
                )
            }
        }

        val uniqueBirthdays = birthdaysToday
            .distinctBy { listOf(it.firstName, it.lastName, it.unit) }
            .sortedWith(compareBy(BirthdayInfo::fullName, BirthdayInfo::unit))
        if (uniqueBirthdays.isEmpty()) return Result.success()

        val fingerprint = uniqueBirthdays.joinToString("|") {
            "${it.fullName.lowercase()}:${it.unit.orEmpty().lowercase()}"
        }
        if (prefs.getString(LAST_NOTIFICATION_DATE_KEY, null) == today.toString() &&
            prefs.getString(LAST_NOTIFICATION_FINGERPRINT_KEY, null) == fingerprint
        ) return Result.success()

        createChannel(context)
        val delivered = try {
            showAggregatedNotification(context, uniqueBirthdays)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Notification delivery failed", error)
            return if (runAttemptCount + 1 < MAX_DELIVERY_ATTEMPTS) {
                shouldScheduleNextRun = false
                Result.retry()
            } else {
                Result.failure()
            }
        }
        if (delivered) {
            prefs.edit {
                putString(LAST_NOTIFICATION_DATE_KEY, today.toString())
                putString(LAST_NOTIFICATION_FINGERPRINT_KEY, fingerprint)
            }
        }

        return Result.success()
    }

    private fun showAggregatedNotification(
        context: Context,
        birthdays: List<BirthdayInfo>
    ): Boolean {
        val title = when (birthdays.size) {
            1 -> "Oggi è il compleanno di ${birthdays[0].fullName}"
            2 -> "Oggi 2 compleanni in unità AGESCI 🎂"
            else -> "Oggi ${birthdays.size} compleanni in unità AGESCI 🎂"
        }

        val content = birthdays.joinToString(" • ") { info ->
            buildString {
                append(info.fullName)
                info.unit?.let { append(" (").append(it).append(")") }
            }
        }

        // Intent to open the app when clicking on the notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // use the default app icon (always available)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.MAGENTA)
            .build()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notification)
            return true
        }
        return false
    }

    private fun createChannel(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Compleanni unità scout",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche di compleanno per le unità selezionate"
        }

        manager.createNotificationChannel(channel)
    }
}

data class BirthdayInfo(
    val firstName: String?,
    val lastName: String?,
    val unit: String?
) {
    val fullName: String
        get() = listOfNotNull(lastName, firstName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { "Senza nome" }
}
