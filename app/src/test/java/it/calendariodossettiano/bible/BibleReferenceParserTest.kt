package it.calendariodossettiano.bible

import org.junit.Assert.assertEquals
import org.junit.Test

class BibleReferenceParserTest {

    private fun spans(ref: String): List<Pair<Int, Pair<String?, String?>>> =
        BibleReferenceParser.parse(ref).flatMap { p ->
            p.spans.map { s -> Triple(p.book, s.chapter, s.startVerse to s.endVerse) }
        }.map { it.second to it.third }

    private fun books(ref: String): List<String> =
        BibleReferenceParser.parse(ref).map { it.book }

    // --- book name extraction ---

    @Test fun simpleBook() = assertEquals(listOf("Nm"), books("Nm 6,22-27"))
    @Test fun bookWithNumber() = assertEquals(listOf("1Sam"), books("1 Sam 16,1-13"))
    @Test fun bookWith2() = assertEquals(listOf("2Cor"), books("2 Cor 3,1-6"))

    // --- simple ranges ---

    @Test fun simpleRange() {
        assertEquals(
            listOf(6 to ("22" to "27")),
            spans("Nm 6,22-27")
        )
    }

    @Test fun singleVerse() {
        assertEquals(listOf(2 to ("3" to "3")), spans("Sof 2,3; 3,12-13").take(1))
    }

    @Test fun wholeChapter() {
        assertEquals(listOf(10 to (null to null)), spans("Est 10"))
    }

    @Test fun verseWithLetterSuffix() {
        assertEquals(listOf(6 to ("17a" to "17a")), spans("Dn 6,17a"))
    }

    // --- cross-chapter (comma notation) ---

    @Test fun crossChapterComma() {
        assertEquals(
            listOf(3 to ("98" to null), 4 to (null to "15")),
            spans("Dn 3,98-4,15")
        )
    }

    @Test fun crossChapterWithLetterStart() {
        assertEquals(
            listOf(8 to ("23b" to null), 9 to (null to "3")),
            spans("Is 8,23b-9,3")
        )
    }

    // --- cross-chapter (dot notation) ---

    @Test fun crossChapterDot() {
        // Gen 1,1-2.2 means Gen 1:1 to Gen 2:2
        assertEquals(
            listOf(1 to ("1" to null), 2 to (null to "2")),
            spans("Gen 1,1-2.2")
        )
    }

    // --- non-consecutive verses (dot separates ranges) ---

    @Test fun nonConsecutiveSinglePlusList() {
        // At 2,14.22-33 → verse 14, then range 22-33
        assertEquals(
            listOf(2 to ("14" to "14"), 2 to ("22" to "33")),
            spans("At 2,14.22-33")
        )
    }

    @Test fun nonConsecutiveWithLetterSuffix() {
        // At 2,14a.22-33 → verse 14a, then range 22-33
        assertEquals(
            listOf(2 to ("14a" to "14a"), 2 to ("22" to "33")),
            spans("At 2,14a.22-33")
        )
    }

    @Test fun nonConsecutiveRanges() {
        // At 8,5-8.14-17 → two ranges in same chapter
        assertEquals(
            listOf(8 to ("5" to "8"), 8 to ("14" to "17")),
            spans("At 8,5-8.14-17")
        )
    }

    // --- + and ; separators ---

    @Test fun plusSeparator() {
        val result = BibleReferenceParser.parse("Gen 2,7-9+3,1-7")
        assertEquals(2, result.size)
        assertEquals("Gen", result[0].book)
        assertEquals("Gen", result[1].book)
        assertEquals(listOf(VerseSpan(2, "7", "9")), result[0].spans)
        assertEquals(listOf(VerseSpan(3, "1", "7")), result[1].spans)
    }

    @Test fun semicolonNoSpace() {
        val result = BibleReferenceParser.parse("Is 7,10-14;8,10c")
        assertEquals(2, result.size)
        assertEquals("Is", result[0].book)
        assertEquals("Is", result[1].book)
        assertEquals(listOf(VerseSpan(7, "10", "14")), result[0].spans)
        assertEquals(listOf(VerseSpan(8, "10c", "10c")), result[1].spans)
    }

    @Test fun semicolonWithSpace() {
        val result = BibleReferenceParser.parse("Sof 2,3; 3,12-13")
        assertEquals(2, result.size)
        assertEquals("Sof", result[0].book)
        assertEquals("Sof", result[1].book)
    }

    // --- letter ranges (Esther deuterocanonical) ---

    @Test fun letterRangeSuffixOnly() {
        // Est 1,1a-r → startVerse=1a, endVerse=1r (normalized from bare "r")
        assertEquals(
            listOf(1 to ("1a" to "1r")),
            spans("Est 1,1a-r")
        )
    }

    @Test fun letterRangeFullLabels() {
        // Est 8,12a-12i* → startVerse=12a, endVerse=12i
        assertEquals(
            listOf(8 to ("12a" to "12i")),
            spans("Est 8,12a-12i*")
        )
    }

    // --- asterisk stripped ---

    @Test fun asteriskStripped() {
        assertEquals(listOf(7 to ("24" to "27")), spans("Lc 7,24-27*"))
    }
}
