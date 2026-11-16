package it.agesci.scoutalert.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in SUPPORTED_ACTIONS) {
            BirthdayScheduler.scheduleNext(context)
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
