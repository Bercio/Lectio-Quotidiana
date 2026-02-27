package it.calendariodossettiano

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(NotificationSettingsActivity.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(NotificationSettingsActivity.KEY_ENABLED, false)) {
            val hour = prefs.getInt(NotificationSettingsActivity.KEY_HOUR, 7)
            val minute = prefs.getInt(NotificationSettingsActivity.KEY_MINUTE, 0)
            NotificationScheduler.schedule(context, hour, minute)
        }
    }
}
