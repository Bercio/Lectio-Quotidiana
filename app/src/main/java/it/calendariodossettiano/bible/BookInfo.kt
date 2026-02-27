package it.calendariodossettiano.bible

/**
 * Maps book abbreviations (as they appear in the JSON, spaces removed) to their
 * testament, which determines the URL path on bibbiaedu.it:
 *   New Testament → /nt/BookAbbrev/
 *   Old Testament → /at/BookAbbrev/
 */
internal object BookInfo {

    private val NEW_TESTAMENT = setOf(
        "1Cor", "1Gv", "1Pt", "1Tm", "1Ts",
        "2Cor", "2Gv", "2Pt", "2Tm", "2Ts",
        "3Gv",
        "Ap", "At",
        "Col",
        "Eb", "Ef",
        "Fil", "Fm",
        "Gal", "Gc", "Gd", "Gv",
        "Lc",
        "Mc", "Mt",
        "Rm",
        "Tt"
    )

    /**
     * Returns the URL path segment for a book, e.g. "at/Nm" or "nt/Lc".
     * The [abbrev] should have spaces already removed (e.g., "1Sam" not "1 Sam").
     */
    fun urlPath(abbrev: String): String {
        val testament = if (abbrev in NEW_TESTAMENT) "nt" else "at"
        return "$testament/$abbrev"
    }
}
