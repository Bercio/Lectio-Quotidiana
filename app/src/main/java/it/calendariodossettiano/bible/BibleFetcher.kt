package it.calendariodossettiano.bible

/**
 * High-level entry point: takes a reference string from calendario.json
 * and returns the full [BibleText] by fetching from bibbiaedu.it.
 *
 * Usage:
 *   val fetcher = BibleFetcher()
 *   val text = fetcher.fetch("Nm 6,22-27")
 *   text.verses.forEach { println("${it.verseLabel}: ${it.text}") }
 */
class BibleFetcher(
    private val scraper: BibleScraper = BibleScraper()
) {
    /**
     * Fetches all verses for [reference] (e.g. "Nm 6,22-27" or "Gen 2,7-9+3,1-7").
     * Chapters are fetched only once even if multiple spans fall within the same chapter.
     */
    suspend fun fetch(reference: String): BibleText {
        val passages = BibleReferenceParser.parse(reference)
        val verses = mutableListOf<Verse>()

        for (passage in passages) {
            // Group spans by chapter to avoid fetching the same chapter twice
            val spansByChapter = passage.spans.groupBy { it.chapter }

            for ((chapter, spans) in spansByChapter) {
                val chapterVerses = scraper.fetchChapter(passage.book, chapter)

                for (span in spans) {
                    chapterVerses
                        .filter { (label, _) -> verseInSpan(label, span) }
                        .forEach { (label, text) ->
                            verses.add(Verse(passage.book, chapter, label, text))
                        }
                }
            }
        }

        return BibleText(reference = reference, verses = verses)
    }

    /**
     * Returns true if [verseLabel] (e.g. "5", "17a") falls within [span].
     * Comparison is numeric-first, then alphabetic on the letter suffix.
     */
    private fun verseInSpan(verseLabel: String, span: VerseSpan): Boolean {
        val v = VerseLabel.parse(verseLabel) ?: return false

        if (span.startVerse != null) {
            val start = VerseLabel.parse(span.startVerse) ?: return false
            if (v < start) return false
        }
        if (span.endVerse != null) {
            val end = VerseLabel.parse(span.endVerse) ?: return false
            if (v > end) return false
        }
        return true
    }
}

/**
 * Comparable verse label: numeric part first, then letter suffix.
 * Examples: "5" → (5, ""), "17a" → (17, "a"), "12i" → (12, "i")
 * Ordering: 5 < 17 < 17a < 17b < 17i < 18
 */
internal data class VerseLabel(val number: Int, val suffix: String) : Comparable<VerseLabel> {
    override fun compareTo(other: VerseLabel): Int {
        val n = number.compareTo(other.number)
        return if (n != 0) n else suffix.compareTo(other.suffix)
    }

    companion object {
        fun parse(label: String): VerseLabel? {
            val numPart = label.takeWhile { it.isDigit() }
            val suffix = label.dropWhile { it.isDigit() }
            val num = numPart.toIntOrNull() ?: return null
            return VerseLabel(num, suffix)
        }
    }
}
