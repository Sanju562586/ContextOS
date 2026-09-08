package com.example.contextos.voice

import com.example.contextos.models.ContextSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ContextMatcherTest {

    private lateinit var matcher: ContextMatcher
    private lateinit var snapshots: List<ContextSnapshot>

    @Before
    fun setUp() {
        matcher = ContextMatcher()
        snapshots = listOf(
            ContextSnapshot(
                id = UUID.randomUUID().toString(),
                name = "Project Alpha",
                description = "Android mobile app workspace"
            ),
            ContextSnapshot(
                id = UUID.randomUUID().toString(),
                name = "Project Alpha V2",
                description = "Refactored second version"
            ),
            ContextSnapshot(
                id = UUID.randomUUID().toString(),
                name = "Project Beta",
                description = "Backend Kotlin service"
            )
        )
    }

    @Test
    fun match_exactMatchCaseInsensitive_returnsExactMatch() {
        val result = matcher.match("project alpha", snapshots)
        assertTrue(result is ContextMatchResult.ExactMatch)
        assertEquals("Project Alpha", (result as ContextMatchResult.ExactMatch).snapshot.name)
    }

    @Test
    fun match_singleMatch_returnsSingleMatch() {
        val result = matcher.match("Beta", snapshots)
        assertTrue(result is ContextMatchResult.SingleMatch)
        assertEquals("Project Beta", (result as ContextMatchResult.SingleMatch).snapshot.name)
    }

    @Test
    fun match_ambiguousMatch_returnsAmbiguousMatchWithCandidates() {
        // "Alpha" could match "Project Alpha" or "Project Alpha V2" if not exact
        // Wait, "Project Alpha" has exact match for "Project Alpha", but for "Alpha":
        val result = matcher.match("Alpha", snapshots)
        assertTrue(result is ContextMatchResult.AmbiguousMatch)
        val ambiguous = result as ContextMatchResult.AmbiguousMatch
        assertEquals("Alpha", ambiguous.query)
        assertEquals(2, ambiguous.candidates.size)
    }

    @Test
    fun match_noMatch_returnsNoMatch() {
        val result = matcher.match("Gamma Nonexistent", snapshots)
        assertTrue(result is ContextMatchResult.NoMatch)
        val noMatch = result as ContextMatchResult.NoMatch
        assertEquals("Gamma Nonexistent", noMatch.query)
        assertEquals(3, noMatch.availableSnapshots.size)
    }

    @Test
    fun match_nullOrEmptyQuery_returnsNoneSpecified() {
        val resultNull = matcher.match(null, snapshots)
        assertTrue(resultNull is ContextMatchResult.NoneSpecified)

        val resultBlank = matcher.match("   ", snapshots)
        assertTrue(resultBlank is ContextMatchResult.NoneSpecified)
    }
}
