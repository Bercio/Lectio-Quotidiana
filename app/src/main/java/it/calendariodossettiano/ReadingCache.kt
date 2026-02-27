package it.calendariodossettiano

import android.content.Context
import it.calendariodossettiano.bible.BibleText
import it.calendariodossettiano.bible.Verse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Persists BibleText objects as JSON files in filesDir/lectio_cache/.
 * One file per date, named "yyyy-MM-dd.json".
 *
 * A "version" file is kept alongside the date files. If it is absent or
 * does not match [CACHE_VERSION], all cached entries are wiped so that
 * stale data from old scraper/parser versions is never served.
 * Bump [CACHE_VERSION] whenever a scraper or parser fix could produce
 * different text for the same reference on the same date.
 */
class ReadingCache(context: Context) {

    companion object {
        private const val CACHE_VERSION = 2
    }

    private val dir = File(context.filesDir, "lectio_cache")

    init {
        dir.mkdirs()
        val versionFile = File(dir, "version")
        val stored = versionFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
        if (stored != CACHE_VERSION) {
            dir.listFiles()?.forEach { it.delete() }
            versionFile.writeText(CACHE_VERSION.toString())
        }
    }

    fun isCached(date: LocalDate): Boolean = fileFor(date).exists()

    fun get(date: LocalDate): BibleText? {
        val file = fileFor(date)
        if (!file.exists()) return null
        return try {
            val obj = JSONObject(file.readText())
            val reference = obj.getString("reference")
            val versesArr = obj.getJSONArray("verses")
            val verses = (0 until versesArr.length()).map { i ->
                val v = versesArr.getJSONObject(i)
                Verse(
                    bookAbbrev = v.getString("bookAbbrev"),
                    chapter    = v.getInt("chapter"),
                    verseLabel = v.getString("verseLabel"),
                    text       = v.getString("text")
                )
            }
            BibleText(reference = reference, verses = verses)
        } catch (e: Exception) {
            file.delete() // corrupt file — discard and refetch
            null
        }
    }

    fun put(date: LocalDate, bibleText: BibleText) {
        val versesArr = JSONArray()
        for (v in bibleText.verses) {
            versesArr.put(JSONObject().apply {
                put("bookAbbrev", v.bookAbbrev)
                put("chapter",    v.chapter)
                put("verseLabel", v.verseLabel)
                put("text",       v.text)
            })
        }
        val obj = JSONObject().apply {
            put("reference", bibleText.reference)
            put("verses", versesArr)
        }
        fileFor(date).writeText(obj.toString())
    }

    fun cachedDates(): Set<LocalDate> =
        dir.listFiles()
            ?.mapNotNull { runCatching { LocalDate.parse(it.nameWithoutExtension) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

    private fun fileFor(date: LocalDate) = File(dir, "$date.json")
}
