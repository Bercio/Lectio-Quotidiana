package it.calendariodossettiano.bible

/**
 * Parses Italian CEI 2008 Bible reference strings (as used in calendario.json)
 * into [ParsedPassage] objects suitable for fetching from bibbiaedu.it.
 *
 * Handled formats:
 *   "Nm 6,22-27"         simple range
 *   "Dn 3,98-4,15"       cross-chapter range (comma notation)
 *   "Is 8,23b-9,3"       cross-chapter range with letter-suffix start
 *   "Gen 1,1-2.2"        cross-chapter range (dot notation in ending)
 *   "Gen 2,7-9+3,1-7"    two passages joined by +
 *   "Is 7,10-14;8,10c"   two passages joined by ;
 *   "Sof 2,3; 3,12-13"   two passages joined by ; (with space)
 *   "At 2,14.22-33"      non-consecutive verses within same chapter
 *   "At 8,5-8.14-17"     non-consecutive ranges within same chapter
 *   "At 2,14a.22-33"     non-consecutive with letter-suffix verse
 *   "Est 10"             whole chapter
 *   "Est 1,1a-r"         letter-range (deuterocanonical sub-verses)
 *   "Est 8,12a-12i*"     letter-range with asterisk (stripped)
 *   "1 Sam 16,1-13"      book name with number prefix
 *   "Lc 7,24-27*"        asterisk stripped
 */
object BibleReferenceParser {

    // Matches an optional leading digit + letters at the start → book abbreviation
    private val BOOK_RE = Regex("""^(\d?\s*[A-Za-z]+)\s*(.*)$""")

    fun parse(reference: String): List<ParsedPassage> {
        val clean = reference.replace("*", "").trim()

        // Split at "+" or ";" to get independent sub-passages
        val segments = clean.split(Regex("[+;]"))

        val passages = mutableListOf<ParsedPassage>()
        var currentBook = ""

        for (rawSegment in segments) {
            val segment = rawSegment.trim()
            if (segment.isEmpty()) continue

            val bookMatch = BOOK_RE.matchEntire(segment)
            if (bookMatch != null) {
                currentBook = bookMatch.groupValues[1].replace(Regex("\\s+"), "")
                val rest = bookMatch.groupValues[2].trim()
                val spans = parseChapterAndVerses(rest)
                passages.add(ParsedPassage(book = currentBook, spans = spans))
            } else {
                // No book prefix — continuation using the last book seen
                val spans = parseChapterAndVerses(segment)
                passages.add(ParsedPassage(book = currentBook, spans = spans))
            }
        }

        return passages
    }

    /**
     * Parses the chapter+verse portion of a segment, e.g.:
     *   "6,22-27"    → [{ch6, 22–27}]
     *   "10"         → [{ch10, null–null}]
     *   "3,98-4,15"  → [{ch3, 98–null}, {ch4, null–15}]
     *   "2,14.22-33" → [{ch2, 14–14}, {ch2, 22–33}]
     */
    private fun parseChapterAndVerses(spec: String): List<VerseSpan> {
        if (spec.isEmpty()) return emptyList()

        val commaIdx = spec.indexOf(',')
        if (commaIdx == -1) {
            val chapter = spec.toIntOrNull() ?: return emptyList()
            return listOf(VerseSpan(chapter, null, null))
        }

        val chapter = spec.substring(0, commaIdx).toIntOrNull() ?: return emptyList()
        val verseSpec = spec.substring(commaIdx + 1)
        return parseVerseSpec(chapter, verseSpec)
    }

    /**
     * Parses the verse specification within a known chapter:
     * - No dot → simple or cross-chapter (comma in end)
     * - Dot where AFTER-part has a hyphen → non-consecutive ranges (split at all dots)
     * - Dot where AFTER-part has NO hyphen → cross-chapter dot notation (rewrite as comma)
     */
    private fun parseVerseSpec(chapter: Int, verseSpec: String): List<VerseSpan> {
        val dotIdx = verseSpec.indexOf('.')
        if (dotIdx == -1) {
            return parseRange(chapter, verseSpec)
        }

        val afterDot = verseSpec.substring(dotIdx + 1)
        return if (afterDot.contains('-')) {
            // Non-consecutive: "14.22-33" or "5-8.14-17" → split at every dot
            verseSpec.split('.').flatMap { part -> parseRange(chapter, part) }
        } else {
            // Cross-chapter dot: "1-2.2" means verse 1 to chapter 2 verse 2
            // Rewrite the last dot as a comma so parseRange handles it
            val lastDot = verseSpec.lastIndexOf('.')
            val rewritten = verseSpec.substring(0, lastDot) + "," + verseSpec.substring(lastDot + 1)
            parseRange(chapter, rewritten)
        }
    }

    /**
     * Parses a single verse range string within [chapter]:
     *   "22-27"   → VerseSpan(chapter, "22", "27")
     *   "17a"     → VerseSpan(chapter, "17a", "17a")
     *   "98-4,15" → VerseSpan(chapter, "98", null) + VerseSpan(4, null, "15")  [cross-chapter]
     *   "1a-r"    → VerseSpan(chapter, "1a", "1r")   [suffix-only end normalized]
     */
    private fun parseRange(chapter: Int, spec: String): List<VerseSpan> {
        val hyphenIdx = spec.indexOf('-')
        if (hyphenIdx == -1) {
            return listOf(VerseSpan(chapter, spec, spec))
        }

        val startVerse = spec.substring(0, hyphenIdx)
        val endSpec = spec.substring(hyphenIdx + 1)

        // Cross-chapter: endSpec contains a comma, e.g. "4,15"
        val endCommaIdx = endSpec.indexOf(',')
        if (endCommaIdx != -1) {
            val endChapter = endSpec.substring(0, endCommaIdx).toIntOrNull()
                ?: return listOf(VerseSpan(chapter, startVerse, endSpec))
            val endVerse = endSpec.substring(endCommaIdx + 1)
            return listOf(
                VerseSpan(chapter, startVerse, null),
                VerseSpan(endChapter, null, endVerse)
            )
        }

        // If endSpec is a bare letter suffix (e.g. "r" in "1a-r"), prepend the
        // numeric part of startVerse to make it a full label (e.g. "1r")
        val normalizedEnd = if (endSpec.all { it.isLetter() }) {
            startVerse.takeWhile { it.isDigit() } + endSpec
        } else {
            endSpec
        }

        return listOf(VerseSpan(chapter, startVerse, normalizedEnd))
    }
}
