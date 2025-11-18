package it.agesci.scoutalert.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.agesci.scoutalert.MainActivity
import it.agesci.scoutalert.R
import it.agesci.scoutalert.widget.BirthdayWidgetProvider
import org.json.JSONArray
import java.time.LocalDate

class BirthdayWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "birthdays_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("birthdays_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("birthdays_json", null) ?: return Result.success()

        val unitsWithNotifications =
            prefs.getStringSet("units_with_notifications", emptySet()) ?: emptySet()

        val today = LocalDate.now()
        val birthdaysToday = mutableListOf<BirthdayInfo>()

        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val day = o.getInt("day")
            val month = o.getInt("month")
            val unit = if (o.has("unit")) o.getString("unit") else null

            val matchesDate = day == today.dayOfMonth && month == today.monthValue
            val matchesUnit =
                unitsWithNotifications.isEmpty() ||
                        (unit != null && unitsWithNotifications.contains(unit))

            if (matchesDate && matchesUnit) {
                birthdaysToday += BirthdayInfo(
                    firstName = o.optString("firstName"),
                    lastName = o.optString("lastName"),
                    unit = unit
                )
            }
        }

        if (birthdaysToday.isEmpty()) return Result.success()

        createChannel(context)
        showAggregatedNotification(context, birthdaysToday)

        BirthdayWidgetProvider.updateAllWidgets(context)

        return Result.success()
    }

    private fun showAggregatedNotification(
        context: Context,
        birthdays: List<BirthdayInfo>
    ) {
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

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
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
}