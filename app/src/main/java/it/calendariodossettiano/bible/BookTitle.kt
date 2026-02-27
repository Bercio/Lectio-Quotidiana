package it.calendariodossettiano.bible

/**
 * Maps CEI 2008 book abbreviations (spaces removed, as used in calendario.json)
 * to the Italian liturgical title used at Mass for announcing readings,
 * e.g. "Dal Vangelo secondo Luca", "Dal libro del profeta Isaia".
 */
object BookTitle {

    private val TITLES = mapOf(
        // ── Pentateuch ────────────────────────────────────────────────────────
        "Gen"  to "Dal libro della Genesi",
        "Es"   to "Dal libro dell'Esodo",
        "Lv"   to "Dal libro del Levitico",
        "Nm"   to "Dal libro dei Numeri",
        "Dt"   to "Dal libro del Deuteronomio",

        // ── Historical books ──────────────────────────────────────────────────
        "Gs"   to "Dal libro di Giosuè",
        "Gdc"  to "Dal libro dei Giudici",
        "Rt"   to "Dal libro di Rut",
        "1Sam" to "Dal primo libro di Samuele",
        "2Sam" to "Dal secondo libro di Samuele",
        "1Re"  to "Dal primo libro dei Re",
        "2Re"  to "Dal secondo libro dei Re",
        "1Cr"  to "Dal primo libro delle Cronache",
        "2Cr"  to "Dal secondo libro delle Cronache",
        "Esd"  to "Dal libro di Esdra",
        "Ne"   to "Dal libro di Neemia",
        "Tb"   to "Dal libro di Tobia",
        "Gdt"  to "Dal libro di Giuditta",
        "Est"  to "Dal libro di Ester",
        "1Mac" to "Dal primo libro dei Maccabei",
        "2Mac" to "Dal secondo libro dei Maccabei",

        // ── Wisdom books ──────────────────────────────────────────────────────
        "Gb"   to "Dal libro di Giobbe",
        "Sal"  to "Dal libro dei Salmi",
        "Pr"   to "Dal libro dei Proverbi",
        "Qo"   to "Dal libro del Qoèlet",
        "Ct"   to "Dal libro del Cantico dei Cantici",
        "Sir"  to "Dal libro del Siracide",
        "Sap"  to "Dal libro della Sapienza",

        // ── Major prophets ────────────────────────────────────────────────────
        "Is"   to "Dal libro del profeta Isaia",
        "Ger"  to "Dal libro del profeta Geremia",
        "Lam"  to "Dal libro delle Lamentazioni",
        "Bar"  to "Dal libro del profeta Baruc",
        "Ez"   to "Dal libro del profeta Ezechiele",
        "Dn"   to "Dal libro del profeta Daniele",

        // ── Minor prophets ────────────────────────────────────────────────────
        "Os"   to "Dal libro del profeta Osea",
        "Gl"   to "Dal libro del profeta Gioele",
        "Am"   to "Dal libro del profeta Amos",
        "Abd"  to "Dal libro del profeta Abdia",
        "Gn"   to "Dal libro del profeta Giona",
        "Mi"   to "Dal libro del profeta Michea",
        "Na"   to "Dal libro del profeta Naum",
        "Ab"   to "Dal libro del profeta Abacuc",
        "Sof"  to "Dal libro del profeta Sofonia",
        "Ag"   to "Dal libro del profeta Aggeo",
        "Zc"   to "Dal libro del profeta Zaccaria",
        "Ml"   to "Dal libro del profeta Malachia",

        // ── Gospels ───────────────────────────────────────────────────────────
        "Mt"   to "Dal Vangelo secondo Matteo",
        "Mc"   to "Dal Vangelo secondo Marco",
        "Lc"   to "Dal Vangelo secondo Luca",
        "Gv"   to "Dal Vangelo secondo Giovanni",

        // ── Acts ──────────────────────────────────────────────────────────────
        "At"   to "Dagli Atti degli Apostoli",

        // ── Pauline letters ───────────────────────────────────────────────────
        "Rm"   to "Dalla lettera di san Paolo ai Romani",
        "1Cor" to "Dalla prima lettera di san Paolo ai Corinzi",
        "2Cor" to "Dalla seconda lettera di san Paolo ai Corinzi",
        "Gal"  to "Dalla lettera di san Paolo ai Galati",
        "Ef"   to "Dalla lettera di san Paolo agli Efesini",
        "Fil"  to "Dalla lettera di san Paolo ai Filippesi",
        "Col"  to "Dalla lettera di san Paolo ai Colossesi",
        "1Ts"  to "Dalla prima lettera di san Paolo ai Tessalonicesi",
        "2Ts"  to "Dalla seconda lettera di san Paolo ai Tessalonicesi",
        "1Tm"  to "Dalla prima lettera di san Paolo a Timoteo",
        "2Tm"  to "Dalla seconda lettera di san Paolo a Timoteo",
        "Tt"   to "Dalla lettera di san Paolo a Tito",
        "Fm"   to "Dalla lettera di san Paolo a Filemone",
        "Eb"   to "Dalla lettera agli Ebrei",

        // ── Catholic letters ──────────────────────────────────────────────────
        "Gc"   to "Dalla lettera di san Giacomo",
        "1Pt"  to "Dalla prima lettera di san Pietro",
        "2Pt"  to "Dalla seconda lettera di san Pietro",
        "1Gv"  to "Dalla prima lettera di san Giovanni",
        "2Gv"  to "Dalla seconda lettera di san Giovanni",
        "3Gv"  to "Dalla terza lettera di san Giovanni",
        "Gd"   to "Dalla lettera di san Giuda",

        // ── Revelation ────────────────────────────────────────────────────────
        "Ap"   to "Dal libro dell'Apocalisse di san Giovanni"
    )

    /**
     * Returns the liturgical reading title for [bookAbbrev] (spaces already removed),
     * or null if the abbreviation is not recognised.
     */
    fun titleFor(bookAbbrev: String): String? = TITLES[bookAbbrev]
}
