package it.calendariodossettiano

import android.graphics.Color
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import it.calendariodossettiano.bible.BibleReferenceParser
import it.calendariodossettiano.bible.BibleText
import it.calendariodossettiano.bible.BookTitle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ReadingViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var progress: ProgressBar
    private lateinit var scrollContent: ScrollView
    private lateinit var tvBookTitle: TextView
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
        tvBookTitle = findViewById(R.id.tvBookTitle)
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

        val prefs = getSharedPreferences(NotificationSettingsActivity.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(NotificationSettingsActivity.KEY_AUTO_CACHE, true)) {
            viewModel.silentPrefetch(currentDate)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_CALENDAR, 0, getString(R.string.action_calendar))
            .setIcon(R.drawable.ic_calendar)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, MENU_NOTIFICATIONS, 1, getString(R.string.action_notifications))
            .setIcon(R.drawable.ic_settings)
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
        val cachedDates = viewModel.getCachedDates()
        val minDate = LocalDate.of(2026, 1, 1)
        val maxDate = LocalDate.of(2026, 12, 31)
        val minMonth = YearMonth.of(2026, 1)
        val maxMonth = YearMonth.of(2026, 12)

        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        val tvMonth   = view.findViewById<TextView>(R.id.tvMonthYear)
        val btnPrev   = view.findViewById<TextView>(R.id.btnPrevMonth)
        val btnNext   = view.findViewById<TextView>(R.id.btnNextMonth)
        val container = view.findViewById<LinearLayout>(R.id.containerDays)

        val displayMonth = arrayOf(YearMonth.from(currentDate))
        val dialog = AlertDialog.Builder(this).setView(view).create()

        val colorGreenLight = ContextCompat.getColor(this, R.color.cached_day)
        val colorToday      = ContextCompat.getColor(this, R.color.today_day)
        val colorSelected   = ContextCompat.getColor(this, R.color.selected_day)
        val colorBody       = ContextCompat.getColor(this, R.color.body_text)
        val colorWhite      = ContextCompat.getColor(this, R.color.on_primary)
        val colorGray       = ContextCompat.getColor(this, R.color.secondary_text)
        val today           = LocalDate.now()
        val cellPx    = (40 * resources.displayMetrics.density).toInt()
        val insetPx   = (4  * resources.displayMetrics.density).toInt()
        val strokePx  = (3  * resources.displayMetrics.density).toInt()

        fun makeCircle(fill: Int, ring: Int? = null) = InsetDrawable(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                if (ring != null) setStroke(strokePx, ring)
            },
            insetPx
        )

        fun updateCalendar() {
            val month = displayMonth[0]
            btnPrev.alpha = if (month > minMonth) 1f else 0.25f
            btnNext.alpha = if (month < maxMonth) 1f else 0.25f
            val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
            tvMonth.text = month.atDay(1).format(fmt).replaceFirstChar { it.uppercase() }

            container.removeAllViews()
            val offset = month.atDay(1).dayOfWeek.value - 1 // Mon=0..Sun=6
            val daysInMonth = month.lengthOfMonth()
            val rows = (offset + daysInMonth + 6) / 7

            for (row in 0 until rows) {
                val rowView = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, cellPx
                    )
                }
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - offset + 1
                    val cell = TextView(this).apply {
                        gravity = Gravity.CENTER
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                        )
                    }
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        val inRange = !date.isBefore(minDate) && !date.isAfter(maxDate)
                        cell.text = dayNum.toString()
                        val isSelected = date == currentDate && inRange
                        when {
                            date == today && inRange -> {
                                cell.background = makeCircle(colorToday, if (isSelected) colorSelected else null)
                                cell.setTextColor(colorWhite)
                            }
                            inRange && date in cachedDates -> {
                                cell.background = makeCircle(colorGreenLight, if (isSelected) colorSelected else null)
                                cell.setTextColor(colorWhite)
                            }
                            isSelected -> {
                                cell.background = makeCircle(Color.TRANSPARENT, colorSelected)
                                cell.setTextColor(colorBody)
                            }
                            inRange -> {
                                cell.setTextColor(colorBody)
                            }
                            else -> {
                                cell.setTextColor(colorGray)
                            }
                        }
                        if (inRange) {
                            cell.setOnClickListener {
                                currentDate = date
                                updateToolbarDate(currentDate)
                                viewModel.loadDate(currentDate)
                                dialog.dismiss()
                            }
                        }
                    }
                    rowView.addView(cell)
                }
                container.addView(rowView)
            }
        }

        btnPrev.setOnClickListener {
            if (displayMonth[0] > minMonth) {
                displayMonth[0] = displayMonth[0].minusMonths(1)
                updateCalendar()
            }
        }
        btnNext.setOnClickListener {
            if (displayMonth[0] < maxMonth) {
                displayMonth[0] = displayMonth[0].plusMonths(1)
                updateCalendar()
            }
        }

        updateCalendar()
        dialog.show()
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
                val bookAbbrev = BibleReferenceParser.parse(state.reference).firstOrNull()?.book
                tvBookTitle.text = bookAbbrev?.let { BookTitle.titleFor(it) } ?: ""
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
