package com.example.moviewather.data.util

import com.example.moviewather.data.local.entity.Movie
import java.util.Locale

/**
 * Result of checking a candidate movie title against existing movies.
 *
 * Implements the duplicate identification hierarchy from Section 9:
 * 1. External movie ID match (Very strong duplicate)
 * 2. Exact title + release year match (Strong duplicate)
 * 3. Normalized title match (Possible duplicate, unless release years are proven different)
 * 4. Similar title warning (Warning only)
 */
sealed interface DuplicateCheckResult {
    /** No match or conflict found. Safe to add without warning. */
    data object None : DuplicateCheckResult

    /**
     * Level 1: External provider ID match (Very strong duplicate).
     * The exact identified movie record already exists.
     */
    data class ExternalIdMatch(
        val existingMovie: Movie,
        val isWatched: Boolean,
        val externalId: String,
    ) : DuplicateCheckResult

    /**
     * Level 2: Exact title and matching release year (Strong duplicate).
     * e.g., Dune (2021) vs existing Dune (2021).
     */
    data class ExactTitleAndYearMatch(
        val existingMovie: Movie,
        val isWatched: Boolean,
        val releaseYear: Int,
    ) : DuplicateCheckResult

    /**
     * Level 3: Exact title match when release years are not specified or conflict-free.
     */
    data class ExactMatch(
        val existingMovie: Movie,
        val isWatched: Boolean,
    ) : DuplicateCheckResult

    /**
     * Level 4: Similar title found (related franchise, sequel, punctuation variance, or word overlap).
     */
    data class SimilarMatch(
        val existingMovie: Movie,
        val reason: String,
    ) : DuplicateCheckResult
}

/**
 * Title normalizer and hierarchy duplicate detector for movie collections.
 *
 * Operates completely offline.
 */
object MovieDuplicateDetector {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "of", "and", "in", "to", "for", "on", "with", "at", "by", "from"
    )

    /**
     * Normalizes a title for duplicate checking:
     * - Trims leading and trailing whitespace
     * - Collapses multiple whitespace characters into a single space
     * - Converts to lower case
     */
    fun normalize(title: String): String {
        return title
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    /**
     * Strips all punctuation and special characters, leaving only alphanumeric words.
     */
    fun stripPunctuation(title: String): String {
        return normalize(title).replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Checks candidate movie against existing movies using the duplicate hierarchy:
     * 1. External ID match
     * 2. Exact title + Release Year match
     * 3. Normalized title match (distinguishing different release years like Dune 1984 vs 2021)
     * 4. Similar title warning
     */
    fun check(
        title: String,
        existingMovies: List<Movie>,
        candidateYear: Int? = null,
        candidateExternalId: String? = null,
    ): DuplicateCheckResult {
        val normalizedCandidate = normalize(title)
        if (normalizedCandidate.isEmpty()) return DuplicateCheckResult.None

        // 1. Level 1: External movie ID match (Very strong duplicate)
        if (!candidateExternalId.isNullOrBlank()) {
            val idMatch = existingMovies.firstOrNull { it.externalId == candidateExternalId }
            if (idMatch != null) {
                return DuplicateCheckResult.ExternalIdMatch(
                    existingMovie = idMatch,
                    isWatched = idMatch.isWatched,
                    externalId = candidateExternalId,
                )
            }
        }

        // 2. Level 2: Exact title + Release Year match (Strong duplicate)
        if (candidateYear != null) {
            val titleAndYearMatch = existingMovies.firstOrNull {
                normalize(it.title) == normalizedCandidate && it.releaseYear == candidateYear
            }
            if (titleAndYearMatch != null) {
                return DuplicateCheckResult.ExactTitleAndYearMatch(
                    existingMovie = titleAndYearMatch,
                    isWatched = titleAndYearMatch.isWatched,
                    releaseYear = candidateYear,
                )
            }
        }

        // 3. Level 3: Normalized title match
        val strippedCandidate = stripPunctuation(title)
        val exactTitleMatches = existingMovies.filter {
            normalize(it.title) == normalizedCandidate || stripPunctuation(it.title) == strippedCandidate
        }

        if (exactTitleMatches.isNotEmpty()) {
            // If the candidate has a known release year, and ALL matching existing movies have
            // a DIFFERENT known release year (e.g. candidate is Dune (2021) and existing is Dune (1984)),
            // they are DIFFERENT MOVIES. They should NOT automatically be considered the same movie!
            val isDifferentYearMovie = candidateYear != null && exactTitleMatches.all {
                it.releaseYear != null && it.releaseYear != candidateYear
            }

            if (!isDifferentYearMovie) {
                val match = exactTitleMatches.first()
                return DuplicateCheckResult.ExactMatch(
                    existingMovie = match,
                    isWatched = match.isWatched,
                )
            }
        }

        // 4. Level 4: Check for similar titles (conservative title-based heuristics)
        val candidateWords = extractSignificantWords(normalizedCandidate)

        for (existing in existingMovies) {
            val normalizedExisting = normalize(existing.title)

            // If existing movie has the exact same title but a proven different year, skip similar warning
            if (normalizedCandidate == normalizedExisting && candidateYear != null && existing.releaseYear != null && candidateYear != existing.releaseYear) {
                continue
            }

            val existingWords = extractSignificantWords(normalizedExisting)

            // Case A: One title contains the other as a whole word / prefix
            // (e.g., "Dune" in "Dune Part Two", "Batman" in "The Batman")
            if (isSubstringRelation(normalizedCandidate, normalizedExisting)) {
                return DuplicateCheckResult.SimilarMatch(
                    existingMovie = existing,
                    reason = "Title is closely related to existing \"${existing.title}\"",
                )
            }

            // Case B: Significant word overlap (e.g., "Batman Begins" vs "The Batman")
            val commonWords = candidateWords.intersect(existingWords)
            if (commonWords.isNotEmpty()) {
                val hasLongCommonWord = commonWords.any { it.length >= 4 }
                if (hasLongCommonWord) {
                    return DuplicateCheckResult.SimilarMatch(
                        existingMovie = existing,
                        reason = "Shares key title word (${commonWords.first()}) with \"${existing.title}\"",
                    )
                }
            }

            // Case C: Small Levenshtein edit distance for close typos (e.g., "Spiderman" vs "Spider-man")
            if (Math.abs(strippedCandidate.length - stripPunctuation(existing.title).length) <= 2) {
                val distance = levenshteinDistance(strippedCandidate, stripPunctuation(existing.title))
                if (distance in 1..2 && strippedCandidate.length >= 5) {
                    return DuplicateCheckResult.SimilarMatch(
                        existingMovie = existing,
                        reason = "Spelling is very similar to \"${existing.title}\"",
                    )
                }
            }
        }

        return DuplicateCheckResult.None
    }

    private fun extractSignificantWords(normalizedTitle: String): Set<String> {
        return normalizedTitle
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(" ")
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .toSet()
    }

    private fun isSubstringRelation(a: String, b: String): Boolean {
        if (a.length < 3 || b.length < 3 || a.length == b.length) return false
        val (shorter, longer) = if (a.length < b.length) a to b else b to a

        // Ensure the shorter title appears with word boundary semantics
        val regex = Regex("(^|\\s)${Regex.escape(shorter)}(\\s|\$|:)")
        return regex.containsMatchIn(longer)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var prev = IntArray(rhs.length + 1) { it }
        var curr = IntArray(rhs.length + 1)

        for (i in lhs.indices) {
            curr[0] = i + 1
            for (j in rhs.indices) {
                val cost = if (lhs[i] == rhs[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[rhs.length]
    }
}
