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
