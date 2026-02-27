package it.calendariodossettiano

import android.content.Context
import it.calendariodossettiano.bible.BibleFetcher
import it.calendariodossettiano.bible.BibleText
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarioRepository(private val context: Context) {

    private val fetcher = BibleFetcher()
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

    suspend fun fetchText(reference: String): BibleText = fetcher.fetch(reference)
}
