package it.calendariodossettiano

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)

        val today = LocalDate.now()
        val reference = CalendarioRepository(context).referenceFor(today)

        val dateStr = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN))
        val title = context.getString(R.string.notif_title, dateStr)
        val body = reference ?: context.getString(R.string.error_no_reading)

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        // Re-schedule for tomorrow
        val prefs = context.getSharedPreferences(NotificationSettingsActivity.PREFS, Context.MODE_PRIVATE)
        val hour = prefs.getInt(NotificationSettingsActivity.KEY_HOUR, 7)
        val minute = prefs.getInt(NotificationSettingsActivity.KEY_MINUTE, 0)
        NotificationScheduler.schedule(context, hour, minute)
    }

    private fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_description)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "lectio_channel"
        const val NOTIFICATION_ID = 1
    }
}
