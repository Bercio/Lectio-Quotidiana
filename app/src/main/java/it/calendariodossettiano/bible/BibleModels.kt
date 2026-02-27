package it.calendariodossettiano.bible

/** A single Bible verse with its text. */
data class Verse(
    val bookAbbrev: String,
    val chapter: Int,
    val verseLabel: String,  // e.g. "1", "17a", "12b"
    val text: String
)

/** The full fetched text for a Bible reference (possibly multiple passages). */
data class BibleText(
    val reference: String,   // original reference string, e.g. "Nm 6,22-27"
    val verses: List<Verse>
)

/** A contiguous verse range within a single chapter. null means open-ended. */
data class VerseSpan(
    val chapter: Int,
    val startVerse: String?,   // null = from the first verse of the chapter
    val endVerse: String?      // null = to the last verse of the chapter
)

/** A parsed Bible passage: a book plus one or more verse spans to fetch. */
data class ParsedPassage(
    val book: String,          // abbreviation without spaces, e.g. "Nm", "1Sam"
    val spans: List<VerseSpan>
)
