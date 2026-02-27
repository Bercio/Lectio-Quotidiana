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
 * Each verse appears in one of two HTML forms:
 *
 *  A) Comment-based (most verses):
 *     <span class="text-to-speech">
 *       <sup><span class="verse_number">N</span></sup><!--<sup>N</sup>-->TEXT
 *     </span>
 *
 *  B) No-comment (some verses, e.g. Est 1d / 1m / 1r):
 *     <sup><span class="verse_number">N</span></sup>
 *     <span class="text-to-speech">TEXT</span>
 *
 * The CEI 2008 editorial brackets ⌈/⌉ appear as inline span elements:
 *   <span class="gamma">&#8968;</span>          →  ⌈
 *   <span class="gammarovesciata">&#8969;</span> →  ⌉
 *
 * We unwrap those spans before running the verse regexes; otherwise their
 * inner </span> causes the non-greedy (.*?) to terminate prematurely.
 */
class BibleScraper(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val BASE = "https://www.bibbiaedu.it/CEI2008"

        // Unwrap span.gamma / span.gammarovesciata, keeping their raw content
        // (the numeric entity &#8968;/&#8969;) so cleanText can decode it later.
        private val GAMMA_RE = Regex(
            """<span class="gamma">(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        private val GAMMA_REV_RE = Regex(
            """<span class="gammarovesciata">(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        // Form A: HTML comment marker followed by verse text.
        private val VERSE_RE = Regex(
            """<!--<sup>(\d+[a-z]*)</sup>-->(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        // Form B: verse number outside the text-to-speech span (no comment marker).
        private val VERSE_TEXT_SPEECH_RE = Regex(
            """<span class="verse_number">(\d+[a-z]*)</span></sup>\s+<span class="text-to-speech">(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        private val TAG_RE = Regex("<[^>]+>")

        // HTML entities we need to unescape
        private val ENTITIES = mapOf(
            "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
            "&quot;" to "\"", "&#039;" to "'", "&nbsp;" to " ",
            "&agrave;" to "à", "&egrave;" to "è", "&igrave;" to "ì",
            "&ograve;" to "ò", "&ugrave;" to "ù", "&Agrave;" to "À",
            "&Egrave;" to "È", "&eacute;" to "é", "&Eacute;" to "É",
            // Defensive: named-entity form of the editorial brackets
            "&lceil;" to "⌈", "&rceil;" to "⌉"
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
        // Unwrap gamma/gammarovesciata spans so their </span> does not cause
        // VERSE_RE to terminate prematurely inside a verse.
        val processed = html
            .replace(GAMMA_RE, "$1")
            .replace(GAMMA_REV_RE, "$1")

        data class Hit(val pos: Int, val label: String, val rawHtml: String)
        val hits = mutableListOf<Hit>()

        // Collect form-A matches (comment-based, the common case)
        for (m in VERSE_RE.findAll(processed)) {
            hits.add(Hit(m.range.first, m.groupValues[1], m.groupValues[2]))
        }
        // Collect form-B matches, skipping any label already found by form A
        val commentLabels = hits.map { it.label }.toSet()
        for (m in VERSE_TEXT_SPEECH_RE.findAll(processed)) {
            val label = m.groupValues[1]
            if (label !in commentLabels) {
                hits.add(Hit(m.range.first, label, m.groupValues[2]))
            }
        }

        hits.sortBy { it.pos }

        val verses = linkedMapOf<String, String>()
        for (hit in hits) {
            val text = cleanText(hit.rawHtml)
            if (text.isNotBlank()) verses[hit.label] = text
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
        // Decode numeric entities like &#8968; — uses toChars() to handle the full Unicode range
        text = text.replace(Regex("&#(\\d+);")) { m ->
            val cp = m.groupValues[1].toIntOrNull()
            if (cp != null && Character.isValidCodePoint(cp)) String(Character.toChars(cp))
            else m.value
        }
        return text.trim()
    }
}

class BibleFetchException(message: String) : Exception(message)
