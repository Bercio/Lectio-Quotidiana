package it.calendariodossettiano

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var switchEnabled: SwitchCompat
    private lateinit var tvTime: TextView
    private lateinit var btnChangeTime: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.notif_settings_title)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        switchEnabled = findViewById(R.id.switchNotifEnabled)
        tvTime = findViewById(R.id.tvTime)
        btnChangeTime = findViewById(R.id.btnChangeTime)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        switchEnabled.isChecked = prefs.getBoolean(KEY_ENABLED, false)
        updateTimeDisplay(prefs.getInt(KEY_HOUR, 7), prefs.getInt(KEY_MINUTE, 0))

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ENABLED, isChecked).apply()
            val h = prefs.getInt(KEY_HOUR, 7)
            val m = prefs.getInt(KEY_MINUTE, 0)
            if (isChecked) NotificationScheduler.schedule(this, h, m)
            else NotificationScheduler.cancel(this)
        }

        btnChangeTime.setOnClickListener {
            val h = prefs.getInt(KEY_HOUR, 7)
            val m = prefs.getInt(KEY_MINUTE, 0)
            TimePickerDialog(this, { _, newHour, newMinute ->
                prefs.edit().putInt(KEY_HOUR, newHour).putInt(KEY_MINUTE, newMinute).apply()
                updateTimeDisplay(newHour, newMinute)
                if (switchEnabled.isChecked) {
                    NotificationScheduler.schedule(this, newHour, newMinute)
                }
            }, h, m, true).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun updateTimeDisplay(hour: Int, minute: Int) {
        tvTime.text = String.format("%02d:%02d", hour, minute)
    }

    companion object {
        const val PREFS = "notif_prefs"
        const val KEY_ENABLED = "notif_enabled"
        const val KEY_HOUR = "notif_hour"
        const val KEY_MINUTE = "notif_minute"
    }
}
