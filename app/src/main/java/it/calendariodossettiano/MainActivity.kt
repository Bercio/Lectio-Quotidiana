package it.calendariodossettiano

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import it.calendariodossettiano.bible.BibleText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ReadingViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var progress: ProgressBar
    private lateinit var scrollContent: ScrollView
    private lateinit var tvReference: TextView
    private lateinit var tvVerses: TextView
    private lateinit var layoutError: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button

    private var currentDate: LocalDate = LocalDate.now()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openNotificationSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        progress = findViewById(R.id.progress)
        scrollContent = findViewById(R.id.scrollContent)
        tvReference = findViewById(R.id.tvReference)
        tvVerses = findViewById(R.id.tvVerses)
        layoutError = findViewById(R.id.layoutError)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)

        setSupportActionBar(toolbar)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        updateToolbarDate(currentDate)

        viewModel = ViewModelProvider(this)[ReadingViewModel::class.java]
        viewModel.state.observe(this) { state -> renderState(state) }
        btnRetry.setOnClickListener { viewModel.loadDate(currentDate) }

        if (savedInstanceState == null) {
            viewModel.loadDate(currentDate)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_CALENDAR, 0, getString(R.string.action_calendar))
            .setIcon(R.drawable.ic_calendar)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, MENU_NOTIFICATIONS, 1, getString(R.string.action_notifications))
            .setIcon(R.drawable.ic_notifications)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_CALENDAR -> { showDatePicker(); true }
            MENU_NOTIFICATIONS -> { handleNotifAction(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateToolbarDate(date: LocalDate) {
        val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ITALIAN)
        supportActionBar?.title = date.format(fmt).replaceFirstChar { it.uppercase() }
    }

    private fun showDatePicker() {
        val minDate = LocalDate.of(2026, 1, 1)
        val maxDate = LocalDate.of(2026, 12, 31)
        val d = currentDate
        val picker = DatePickerDialog(this, { _, year, month, day ->
            currentDate = LocalDate.of(year, month + 1, day)
            updateToolbarDate(currentDate)
            viewModel.loadDate(currentDate)
        }, d.year, d.monthValue - 1, d.dayOfMonth)
        picker.datePicker.minDate = minDate.toEpochDay() * 86_400_000L
        picker.datePicker.maxDate = maxDate.toEpochDay() * 86_400_000L
        picker.show()
    }

    private fun handleNotifAction() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openNotificationSettings()
            } else {
                notifPermissionLauncher.launch(perm)
            }
        } else {
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(this, NotificationSettingsActivity::class.java))
    }

    private fun renderState(state: ReadingState) {
        progress.visibility = View.GONE
        scrollContent.visibility = View.GONE
        layoutError.visibility = View.GONE

        when (state) {
            is ReadingState.Loading -> {
                progress.visibility = View.VISIBLE
            }
            is ReadingState.Success -> {
                scrollContent.visibility = View.VISIBLE
                tvReference.text = state.reference
                tvVerses.text = buildVersesText(state.bibleText)
            }
            is ReadingState.NoReading -> {
                layoutError.visibility = View.VISIBLE
                tvError.text = getString(R.string.error_no_reading)
                btnRetry.visibility = View.GONE
            }
            is ReadingState.Error -> {
                layoutError.visibility = View.VISIBLE
                tvError.text = getString(R.string.error_network)
                btnRetry.visibility = View.VISIBLE
            }
        }
    }

    private fun buildVersesText(bibleText: BibleText): SpannableStringBuilder {
        val green = ContextCompat.getColor(this, R.color.reference_text)
        val sb = SpannableStringBuilder()
        for ((index, verse) in bibleText.verses.withIndex()) {
            if (index > 0) sb.append("\n\n")
            val numStart = sb.length
            sb.append(verse.verseLabel)
            val numEnd = sb.length
            sb.setSpan(ForegroundColorSpan(green), numStart, numEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(0.78f), numStart, numEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\u2002") // en space
            sb.append(verse.text)
        }
        return sb
    }

    companion object {
        private const val MENU_CALENDAR = 1
        private const val MENU_NOTIFICATIONS = 2
    }
}
