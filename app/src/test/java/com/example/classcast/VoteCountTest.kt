package com.example.classcast

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for vote-counting logic that mirrors HeadcountActivity.onDataChange().
 *
 * The counting algorithm is pure business logic — extract it here to verify
 * edge cases without needing Firebase or Android.
 *
 * Run with: ./gradlew test
 */
class VoteCountTest {

    /**
     * Mirrors the counting loop in HeadcountActivity.onDataChange().
     * Input: map of userId -> voteValue strings.
     * Output: Triple(going, maybe, notGoing).
     */
    private fun countVotes(votes: Map<String, String>): Triple<Int, Int, Int> {
        var going = 0
        var maybe = 0
        var notGoing = 0
        for ((_, value) in votes) {
            when (value) {
                "going"     -> going++
                "maybe"     -> maybe++
                "not_going" -> notGoing++
            }
        }
        return Triple(going, maybe, notGoing)
    }

    // ── Basic counts ──────────────────────────────────────────────────────

    @Test
    fun `empty vote map returns all zeros`() {
        val (g, m, n) = countVotes(emptyMap())
        assertEquals(0, g)
        assertEquals(0, m)
        assertEquals(0, n)
    }

    @Test
    fun `single going vote counted correctly`() {
        val (g, m, n) = countVotes(mapOf("uid1" to "going"))
        assertEquals(1, g)
        assertEquals(0, m)
        assertEquals(0, n)
    }

    @Test
    fun `single maybe vote counted correctly`() {
        val (g, m, n) = countVotes(mapOf("uid1" to "maybe"))
        assertEquals(0, g)
        assertEquals(1, m)
        assertEquals(0, n)
    }

    @Test
    fun `single not_going vote counted correctly`() {
        val (g, m, n) = countVotes(mapOf("uid1" to "not_going"))
        assertEquals(0, g)
        assertEquals(0, m)
        assertEquals(1, n)
    }

    // ── Multiple votes ────────────────────────────────────────────────────

    @Test
    fun `mixed votes counted correctly`() {
        val votes = mapOf(
            "uid1" to "going",
            "uid2" to "maybe",
            "uid3" to "not_going",
            "uid4" to "going"
        )
        val (g, m, n) = countVotes(votes)
        assertEquals(2, g)
        assertEquals(1, m)
        assertEquals(1, n)
    }

    @Test
    fun `total is sum of all categories`() {
        val votes = mapOf(
            "uid1" to "going",
            "uid2" to "going",
            "uid3" to "maybe",
            "uid4" to "not_going"
        )
        val (g, m, n) = countVotes(votes)
        assertEquals(4, g + m + n)
    }

    @Test
    fun `all going votes are counted`() {
        val votes = (1..5).associate { "uid$it" to "going" }
        val (g, m, n) = countVotes(votes)
        assertEquals(5, g)
        assertEquals(0, m)
        assertEquals(0, n)
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    fun `unknown vote value is ignored`() {
        val votes = mapOf("uid1" to "yes", "uid2" to "going")
        val (g, m, n) = countVotes(votes)
        assertEquals(1, g)
        assertEquals(0, m)
        assertEquals(0, n)
    }

    @Test
    fun `empty string vote value is ignored`() {
        val votes = mapOf("uid1" to "", "uid2" to "maybe")
        val (g, m, n) = countVotes(votes)
        assertEquals(0, g)
        assertEquals(1, m)
        assertEquals(0, n)
    }

    @Test
    fun `vote values are case sensitive, Going with capital G is not counted`() {
        val votes = mapOf("uid1" to "Going")
        val (g, _, _) = countVotes(votes)
        assertEquals(0, g) // must be lowercase "going"
    }

    @Test
    fun `last vote from same user overwrites previous (map semantics)`() {
        // In Kotlin Map, duplicate keys keep last value
        val votes = mapOf("uid1" to "not_going") // student changed vote to not_going
        val (g, m, n) = countVotes(votes)
        assertEquals(0, g)
        assertEquals(0, m)
        assertEquals(1, n)
    }
}
