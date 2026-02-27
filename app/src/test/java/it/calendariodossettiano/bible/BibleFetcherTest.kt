package it.calendariodossettiano.bible

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleFetcherTest {

    private val fetcher = BibleFetcher()

    @Test
    fun testFetchSimpleRange() = runBlocking {
        printFetchResult("Nm 6,22-27")
    }

    @Test
    fun testFetchCrossChapter() = runBlocking {
        printFetchResult("Dn 3,98-4,15")
    }

    @Test
    fun testFetchMultiplePassagesPlus() = runBlocking {
        printFetchResult("Gen 2,7-9+3,1-7")
    }

    @Test
    fun testFetchMultiplePassagesSemicolon() = runBlocking {
        printFetchResult("Sof 2,3; 3,12-13")
    }

    @Test
    fun testFetchNonConsecutiveRanges() = runBlocking {
        printFetchResult("At 8,5-8.14-17")
    }

    @Test
    fun testFetchDotNotationCrossChapter() = runBlocking {
        printFetchResult("Gen 1,1-2.2")
    }

    @Test
    fun testFetchWithAsterisk() = runBlocking {
        printFetchResult("Lc 7,24-27*")
    }

    @Test
    fun testFetchSpaceInBookName() = runBlocking {
        printFetchResult("1 Sam 16,1-13")
    }

    // --- Esther (Est) tests from calendario.json ---

    @Test fun testEst_1_1a_r_content() = runBlocking {
        val expected = """
            1a Nel secondo anno di regno del grande re Artaserse, il giorno primo di Nisan, Mardocheo,
            figlio di Giàiro, figlio di Simei, figlio di Kis, della tribù di Beniamino,
            ebbe in sogno una visione.
            1b Egli era ⌈un Giudeo che abitava nella città di Susa⌉, un uomo ragguardevole,
            che prestava servizio alla corte del re
            1c ⌈e proveniva dal gruppo degli esuli che Nabucodònosor, re di Babilonia,
            aveva deportato da Gerusalemme con Ieconia, re della Giudea.⌉
            1d Questo fu il suo sogno: ecco, grida e tumulto, tuoni e terremoto,
            sconvolgimenti sulla terra.
            1e Ed ecco: due enormi draghi avanzarono, tutti e due pronti alla lotta,
            [e risuonò potente il loro grido.]
            1f ⌈Al loro grido ogni nazione si preparò alla guerra,
            per combattere contro il popolo dei giusti.⌉
            1g Ecco, un giorno di tenebre e di caligine! Tribolazione e angustia,
            afflizione e grandi sconvolgimenti sulla terra!
            1h ⌈Tutta la nazione dei giusti rimase sconvolta: essi, temendo la propria rovina,
            si prepararono a morire⌉ e levarono a Dio il loro grido.
            1i Ma dal loro grido, come da una piccola fonte, sorse un grande fiume
            con acque abbondanti.
            1k Apparvero la luce e il sole: gli umili furono esaltati e divorarono i superbi.
            1l Mardocheo allora si svegliò: aveva visto questo sogno e quello che Dio aveva
            deciso di fare; in cuor suo continuava a ripensarvi fino a notte,
            cercando di comprenderlo in ogni suo particolare.
            1m ⌈Mardocheo alloggiava alla corte con Gabatà e Tarra, i due eunuchi del re
            che custodivano la corte.
            1n Intese i loro ragionamenti, indagò sui loro disegni e venne a sapere che quelli
            si preparavano a mettere le mani sul re Artaserse. Allora ne avvertì il re.
            1o Il re sottopose i due eunuchi a un interrogatorio: essi confessarono
            e furono tolti di mezzo.
            1p Poi il re fece scrivere questi fatti nelle cronache e anche Mardocheo
            li mise per iscritto.
            1q Il re costituì Mardocheo funzionario della corte e gli fece regali
            in compenso di queste cose.
            1r Ma vi era anche Aman, figlio di Amadàta, il Bugeo, che era molto stimato
            presso il re e cercò il modo di fare del male a Mardocheo e al suo popolo,
            per questa faccenda che riguardava i due eunuchi del re.⌉
        """.trimIndent()

        val result = fetcher.fetch("Est 1,1a-r")
        val actual = result.verses.joinToString("") { v -> v.verseLabel + v.text }

        // Print for diagnosis
        println("\n--- Actual (Est 1,1a-r) ---")
        result.verses.forEach { println("${it.verseLabel}: ${it.text}") }

        val actualNorm   = actual.replace(Regex("\\s+"), "")
        val expectedNorm = expected.replace(Regex("\\s+"), "")

        org.junit.Assert.assertEquals(
            "Est 1,1a-r text mismatch (whitespace-normalised)",
            expectedNorm, actualNorm
        )
    }

    @Test fun testEst_1_1a_r() = runBlocking { printFetchResult("Est 1,1a-r") }
    @Test fun testEst_1_1_22() = runBlocking { printFetchResult("Est 1,1-22") }
    @Test fun testEst_2_1_23() = runBlocking { printFetchResult("Est 2,1-23") }
    @Test fun testEst_3_1_13() = runBlocking { printFetchResult("Est 3,1-13") }
    @Test fun testEst_3_13a_15() = runBlocking { printFetchResult("Est 3,13a-15") }
    @Test fun testEst_4_1_17() = runBlocking { printFetchResult("Est 4,1-17") }
    @Test fun testEst_4_17a_i() = runBlocking { printFetchResult("Est 4,17a-i") }
    @Test fun testEst_4_17k_z() = runBlocking { printFetchResult("Est 4,17k-z") }
    @Test fun testEst_5_1_14() = runBlocking { printFetchResult("Est 5,1-14") }
    @Test fun testEst_6_1_14() = runBlocking { printFetchResult("Est 6,1-14") }
    @Test fun testEst_7_1_10_star() = runBlocking { printFetchResult("Est 7,1-10*") }
    @Test fun testEst_8_1_12_star() = runBlocking { printFetchResult("Est 8,1-12*") }
    @Test fun testEst_8_12a_12i_star() = runBlocking { printFetchResult("Est 8,12a-12i*") }
    @Test fun testEst_8_12k_12t_star() = runBlocking { printFetchResult("Est 8,12k-12t*") }
    @Test fun testEst_8_12u_17_star() = runBlocking { printFetchResult("Est 8,12u-17*") }
    @Test fun testEst_9_1_19() = runBlocking { printFetchResult("Est 9,1-19") }
    @Test fun testEst_9_20_32() = runBlocking { printFetchResult("Est 9,20-32") }
    @Test fun testEst_10() = runBlocking { printFetchResult("Est 10") }

    private suspend fun printFetchResult(reference: String) {
        println("\n--- Fetching: $reference ---")
        try {
            val result = fetcher.fetch(reference)
            result.verses.forEach { verse ->
                println("${verse.bookAbbrev} ${verse.chapter},${verse.verseLabel} ${verse.text}")
            }

            assertTrue("Should have fetched at least one verse for $reference", result.verses.isNotEmpty())

            println("Successfully fetched ${result.verses.size} verses.")
        } catch (e: Exception) {
            println("Error fetching $reference: ${e.message}")
            throw e
        }
    }
}
