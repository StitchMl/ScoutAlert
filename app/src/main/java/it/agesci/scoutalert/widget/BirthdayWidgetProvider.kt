package it.agesci.scoutalert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import it.agesci.scoutalert.MainActivity
import it.agesci.scoutalert.R
import org.json.JSONArray
import java.time.LocalDate

class BirthdayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, BirthdayWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.birthday_widget)

            val prefs =
                context.getSharedPreferences("birthdays_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("birthdays_json", null)
            val unitsWithNotifications =
                prefs.getStringSet("units_with_notifications", emptySet()) ?: emptySet()

            val today = LocalDate.now()
            var subtitle = "Nessun compleanno oggi"
            var body = "Tap to open the app"

            if (json != null) {
                val arr = JSONArray(json)
                val list = mutableListOf<String>()
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
                        val fn = o.optString("firstName")
                        val ln = o.optString("lastName")
                        val name = listOf(ln, fn)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                        val line = if (unit != null) "$name  ($unit)" else name
                        list += line
                    }
                }

                if (list.isNotEmpty()) {
                    subtitle = "Oggi ${list.size} compleanni 🎉"
                    body = list.take(3).joinToString("\n")
                }
            }

            views.setTextViewText(R.id.widgetTitleText, "Compleanni di oggi")
            views.setTextViewText(R.id.widgetSubtitleText, subtitle)
            views.setTextViewText(R.id.widgetListText, body)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}