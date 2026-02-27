package it.calendariodossettiano

import android.content.Context
import it.calendariodossettiano.bible.BibleFetcher
import it.calendariodossettiano.bible.BibleText
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarioRepository(private val context: Context) {

    private val fetcher = BibleFetcher()
    private val cache = ReadingCache(context)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val calendar: Map<String, String> by lazy {
        val json = context.assets.open("calendario.json").bufferedReader().readText()
        val arr = JSONArray(json)
        buildMap {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                put(obj.getString("data"), obj.getString("lectio"))
            }
        }
    }

    fun referenceFor(date: LocalDate): String? = calendar[date.format(formatter)]

    suspend fun fetchText(reference: String, date: LocalDate): BibleText {
        cache.get(date)?.let { return it }
        val text = fetcher.fetch(reference)
        cache.put(date, text)
        return text
    }

    /**
     * Silently pre-fetches the 7 days following [from], skipping any already cached.
     * Network errors for individual dates are swallowed — best-effort only.
     */
    fun cachedDates(): Set<LocalDate> = cache.cachedDates()

    suspend fun prefetchWeek(from: LocalDate) {
        for (i in 1..7) {
            val date = from.plusDays(i.toLong())
            if (cache.isCached(date)) continue
            val reference = referenceFor(date) ?: continue
            try {
                val text = fetcher.fetch(reference)
                cache.put(date, text)
            } catch (_: Exception) {
                // best-effort: ignore failures for individual dates
            }
        }
    }
}
