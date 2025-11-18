package it.agesci.scoutalert.ui

import android.content.Context
import androidx.core.content.edit
import it.agesci.scoutalert.excel.BirthdayEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import it.agesci.scoutalert.widget.BirthdayWidgetProvider
import it.agesci.scoutalert.notifications.BirthdayScheduler

// Logging tag + SharedPreferences keys
const val TAG = "ExcelReader"
const val PREFS_NAME = "birthdays_prefs"
const val PREFS_UNITS_KEY = "units_with_notifications"

// Date formats used for parsing
private val BIRTH_DATE_FORMATS: List<DateTimeFormatter> = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,                // yyyy-MM-dd (dal parser)
    DateTimeFormatter.ofPattern("d/M/uuuu"),
    DateTimeFormatter.ofPattern("dd/MM/uuuu"),
    DateTimeFormatter.ofPattern("d-M-uuuu"),
    DateTimeFormatter.ofPattern("dd-MM-uuuu")
)

fun parseBirthDate(raw: String): LocalDate? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    for (fmt in BIRTH_DATE_FORMATS) {
        try {
            return LocalDate.parse(s, fmt)
        } catch (_: Exception) {
        }
    }
    return null
}

fun normalizeUnitName(raw: String): String {
    val s = raw.trim().lowercase()
    if (s.isEmpty()) return ""

    val noDigits = s.replace(Regex("\\d"), "").trim()
    val base = noDigits
        .replace(".", "")
        .replace("-", " ")
        .replace("/", "")
        .trim()

    return when {
        base.contains("coca") || base.contains("comunit") || base.contains("adult") ->
            "Co.Ca."

        base.contains("lc") || base.contains("l c") ||
                base.contains("lupetti") || base.contains("coccinell") ->
            "L/C"

        base.contains("eg") || base.contains("e g") ||
                base.contains("esploratori") || base.contains("guide") ->
            "E/G"

        base.contains("rs") || base.contains("r s") ||
                base.contains("rover") || base.contains("scolte") ||
                base.contains("noviziato") || base.contains("novizi") ->
            "R/S"

        else -> raw.trim()
    }
}

fun saveBirthdaysToPrefs(context: Context, entries: List<BirthdayEntry>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    entries.forEach { e ->
        val o = JSONObject()
        o.put("firstName", e.firstName)
        o.put("lastName", e.lastName)
        o.put("unit", e.unit)
        o.put("day", e.day)
        o.put("month", e.month)
        if (e.year != null) o.put("year", e.year)
        arr.put(o)
    }
    prefs.edit { putString("birthdays_json", arr.toString()) }

    // 🔁 Update the widgets NOW
    BirthdayWidgetProvider.updateAllWidgets(context)

    // 🔔 IMMEDIATELY launch the worker that checks birthdays (and sends notifications if necessary)
    BirthdayScheduler.triggerNow(context)
}

fun loadBirthdaysFromPrefs(context: Context): List<BirthdayEntry> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString("birthdays_json", null) ?: return emptyList()

    val arr = JSONArray(json)
    val list = mutableListOf<BirthdayEntry>()

    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val firstName = o.optString("firstName")
        val lastName = o.optString("lastName")
        val unit = if (o.has("unit")) o.optString("unit") else null
        val day = o.getInt("day")
        val month = o.getInt("month")
        val year = if (o.has("year")) o.getInt("year") else null

        val fullName = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { "Senza nome" }

        list += BirthdayEntry(
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            unit = unit,
            day = day,
            month = month,
            year = year
        )
    }

    return list
}