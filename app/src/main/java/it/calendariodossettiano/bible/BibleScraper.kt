package it.calendariodossettiano.bible

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches Bible chapter pages from bibbiaedu.it (CEI 2008) and extracts verse text.
 *
 * URL format: https://www.bibbiaedu.it/CEI2008/{at|nt}/{Book}/{chapter}/
 *
 * Each verse in the HTML looks like:
 *   <sup><span class="verse_number">N</span></sup><!--<sup>N</sup>-->TEXT</span>
 * or (for verses with footnotes):
 *   <button ...>N</button><!--<sup>N</sup>-->TEXT</span>
 *
 * We key off the HTML comment <!--<sup>N</sup>--> which is always present,
 * and capture the text that follows up to the closing </span>.
 */
class BibleScraper(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val BASE = "https://www.bibbiaedu.it/CEI2008"

        // Matches the verse marker comment and captures: (1) verse label, (2) raw HTML text
        private val VERSE_RE = Regex(
            """<!--<sup>(\d+[a-z]*)</sup>-->(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        private val TAG_RE = Regex("<[^>]+>")

        // HTML entities we need to unescape
        private val ENTITIES = mapOf(
            "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
            "&quot;" to "\"", "&#039;" to "'", "&nbsp;" to " ",
            "&agrave;" to "à", "&egrave;" to "è", "&igrave;" to "ì",
            "&ograve;" to "ò", "&ugrave;" to "ù", "&Agrave;" to "À",
            "&Egrave;" to "È", "&eacute;" to "é", "&Eacute;" to "É"
        )
    }

    /**
     * Fetches all verses of [chapter] from [bookAbbrev] as a map of verseLabel → text.
     * [bookAbbrev] should have spaces removed (e.g. "1Sam", "Nm").
     */
    suspend fun fetchChapter(bookAbbrev: String, chapter: Int): Map<String, String> =
        withContext(Dispatchers.IO) {
            val urlPath = BookInfo.urlPath(bookAbbrev)
            val url = "$BASE/$urlPath/$chapter/"

            val html = get(url)
            parseVerses(html)
        }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw BibleFetchException("HTTP ${response.code} fetching $url")
        }
        return response.body?.string() ?: throw BibleFetchException("Empty response from $url")
    }

    /** Parses the chapter HTML and returns a map of verse label → cleaned text. */
    private fun parseVerses(html: String): Map<String, String> {
        val verses = linkedMapOf<String, String>()
        for (match in VERSE_RE.findAll(html)) {
            val label = match.groupValues[1]
            val rawHtml = match.groupValues[2]
            val text = cleanText(rawHtml)
            if (text.isNotBlank()) {
                verses[label] = text
            }
        }
        return verses
    }

    private fun cleanText(html: String): String {
        var text = html
            .replace("<br>", " ").replace("<br/>", " ").replace("<br />", " ")
            .replace(TAG_RE, "")
        for ((entity, char) in ENTITIES) {
            text = text.replace(entity, char)
        }
        // Decode numeric entities like &#8220;
        text = text.replace(Regex("&#(\\d+);")) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        return text.trim()
    }
}

class BibleFetchException(message: String) : Exception(message)
