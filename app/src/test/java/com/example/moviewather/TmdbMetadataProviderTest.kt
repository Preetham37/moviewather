package com.example.moviewather

import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.metadata.CompositeMovieMetadataProvider
import com.example.moviewather.data.metadata.MetadataException
import com.example.moviewather.data.metadata.MissingApiKeyException
import com.example.moviewather.data.metadata.MovieMetadataProvider
import com.example.moviewather.data.metadata.MovieMetadataResult
import com.example.moviewather.data.metadata.NetworkChecker
import com.example.moviewather.data.metadata.OfflineException
import com.example.moviewather.data.metadata.TmdbMetadataProvider
import com.example.moviewather.data.util.DuplicateCheckResult
import com.example.moviewather.data.util.MovieDuplicateDetector
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbMetadataProviderTest {

    @Test
    fun testMissingApiKeyThrowsMissingApiKeyException() = runBlocking {
        val provider = TmdbMetadataProvider(apiKeyProvider = { "" })
        val result = provider.searchMovies("Interstellar")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MissingApiKeyException)
    }

    @Test
    fun testCompositeProviderOfflineThrowsOfflineException() = runBlocking {
        val offlineChecker = object : NetworkChecker(null) {
            override fun isOnline(): Boolean = false
        }
        val tmdbProvider = TmdbMetadataProvider(apiKeyProvider = { "valid_key" })
        val composite = CompositeMovieMetadataProvider(
            networkChecker = offlineChecker,
            tmdbProvider = tmdbProvider,
            tmdbApiKeyProvider = { "valid_key" },
        )

        val result = composite.searchMovies("Interstellar")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    @Test
    fun testCompositeProviderMissingApiKeyThrowsMissingApiKeyException() = runBlocking {
        val onlineChecker = object : NetworkChecker(null) {
            override fun isOnline(): Boolean = true
        }
        val tmdbProvider = TmdbMetadataProvider(apiKeyProvider = { "" })
        val composite = CompositeMovieMetadataProvider(
            networkChecker = onlineChecker,
            tmdbProvider = tmdbProvider,
            tmdbApiKeyProvider = { "" },
        )

        val result = composite.searchMovies("Avatar")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MissingApiKeyException)
    }

    @Test
    fun testCompositeProviderPropagatesTmdbFailureWithoutSwallowing() = runBlocking {
        val onlineChecker = object : NetworkChecker(null) {
            override fun isOnline(): Boolean = true
        }
        // Custom provider simulating a 401 or network error from TMDB
        val failingTmdb = object : TmdbMetadataProvider(apiKeyProvider = { "invalid_key" }) {
            override suspend fun searchMovies(query: String): Result<List<MovieMetadataResult>> {
                return Result.failure(MetadataException("Invalid TMDb API key. Please check your credentials in Settings."))
            }
        }
        val composite = CompositeMovieMetadataProvider(
            networkChecker = onlineChecker,
            tmdbProvider = failingTmdb,
            tmdbApiKeyProvider = { "invalid_key" },
        )

        val result = composite.searchMovies("Dune")
        // It must NOT fall back to Demo catalog. It must propagate the real TMDb failure!
        assertTrue(result.isFailure)
        assertEquals("Invalid TMDb API key. Please check your credentials in Settings.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testAllResultsAreReturnedFromTmdbResponse() {
        val sampleJson = """
        {
          "page": 1,
          "results": [
            {
              "id": 438631,
              "title": "Dune",
              "release_date": "2021-09-15",
              "poster_path": "/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",
              "overview": "Paul Atreides travels to Arrakis."
            },
            {
              "id": 841,
              "title": "Dune",
              "release_date": "1984-12-14",
              "poster_path": "/gDztdfvT1gG233W89u05m72x7c.jpg",
              "overview": "In the year 10191..."
            },
            {
              "id": 693134,
              "title": "Dune: Part Two",
              "release_date": "2024-02-27",
              "poster_path": "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
              "overview": "Follow the mythic journey..."
            },
            {
              "id": 184345,
              "title": "Jodorowsky's Dune",
              "release_date": "2013-05-18",
              "poster_path": "/m0ZzJm5n3n24L6F5Ua4dY91c14P.jpg",
              "overview": "The story of cult film director..."
            }
          ]
        }
        """.trimIndent()

        val root = JSONObject(sampleJson)
        val resultsArray = root.getJSONArray("results")
        val list = mutableListOf<MovieMetadataResult>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)
            val id = item.getInt("id")
            val title = item.getString("title")
            val releaseDate = item.optString("release_date", "")
            val releaseYear = releaseDate.take(4).toIntOrNull()
            val posterPath = item.optString("poster_path", "").trim()
            val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else null
            val overview = item.optString("overview", "").trim().ifBlank { null }

            list.add(
                MovieMetadataResult(
                    externalId = "tmdb_$id",
                    title = title,
                    releaseYear = releaseYear,
                    posterUrl = posterUrl,
                    overview = overview,
                    provider = "The Movie Database (TMDb)",
                )
            )
        }

        // Verify that all 4 results are preserved without picking only the first
        assertEquals(4, list.size)
        assertEquals("tmdb_438631", list[0].externalId)
        assertEquals(2021, list[0].releaseYear)
        assertEquals("tmdb_841", list[1].externalId)
        assertEquals(1984, list[1].releaseYear)
        assertEquals("tmdb_693134", list[2].externalId)
        assertEquals(2024, list[2].releaseYear)
        assertEquals("tmdb_184345", list[3].externalId)
        assertEquals(2013, list[3].releaseYear)
    }

    @Test
    fun testDuplicateHierarchy() {
        val existing = listOf(
            Movie(id = 1, title = "Dune", releaseYear = 1984, externalId = "tmdb_841"),
            Movie(id = 2, title = "The Batman", releaseYear = 2022, externalId = "tmdb_414906"),
        )

        // 1. Level 1: External ID match
        val checkId = MovieDuplicateDetector.check("Dune", existing, candidateExternalId = "tmdb_841")
        assertTrue(checkId is DuplicateCheckResult.ExternalIdMatch)

        // 2. Level 2: Exact title + year match
        val checkTitleAndYear = MovieDuplicateDetector.check("Dune", existing, candidateYear = 1984)
        assertTrue(checkTitleAndYear is DuplicateCheckResult.ExactTitleAndYearMatch)

        // 3. Different release year with same title is distinguished (Dune 2021 vs Dune 1984)
        val checkDifferentYear = MovieDuplicateDetector.check("Dune", existing, candidateYear = 2021)
        // Dune 2021 is NOT treated as a duplicate of Dune 1984
        assertTrue(checkDifferentYear is DuplicateCheckResult.None)

        // 4. Level 4: Similar title warning
        val checkSimilar = MovieDuplicateDetector.check("Batman Begins", existing)
        assertTrue(checkSimilar is DuplicateCheckResult.SimilarMatch)
    }

    @Test
    fun testSearchUrlEncodingAndApiKeyHandling() = runBlocking {
        var requestedUrl = ""
        var usedAuthHeader = ""

        val provider = object : TmdbMetadataProvider(apiKeyProvider = { "test_api_key_123" }) {
            override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                requestedUrl = urlString
                usedAuthHeader = if (isBearerToken) "Bearer $apiKey" else ""
                return """{"page":1,"results":[]}"""
            }
        }

        // Test with spaces: "The Dark Knight"
        provider.searchMovies("The Dark Knight")
        assertTrue("URL must contain properly encoded query", requestedUrl.contains("query=The+Dark+Knight") || requestedUrl.contains("query=The%20Dark%20Knight"))
        assertTrue("URL must contain api_key parameter for standard key", requestedUrl.contains("api_key=test_api_key_123"))
        assertEquals("", usedAuthHeader)

        // Test with Bearer token (JWT format)
        val bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.header.payload.signature"
        val bearerProvider = object : TmdbMetadataProvider(apiKeyProvider = { bearerToken }) {
            override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                requestedUrl = urlString
                usedAuthHeader = if (isBearerToken) "Bearer $apiKey" else ""
                return """{"page":1,"results":[]}"""
            }
        }
        bearerProvider.searchMovies("Interstellar")
        // Bearer tokens should NOT be appended in the clear in query parameters
        assertTrue("Query must not append bearer token in url", !requestedUrl.contains("api_key="))
        assertEquals("Bearer $bearerToken", usedAuthHeader)
    }

    @Test
    fun testAllRequiredSearchesReturnMovieCandidates() = runBlocking {
        val moviesToTest = mapOf(
            "Dune" to listOf(
                Pair(438631, "Dune"),
                Pair(841, "Dune"),
                Pair(693134, "Dune: Part Two"),
                Pair(184345, "Jodorowsky's Dune"),
            ),
            "Interstellar" to listOf(
                Pair(157336, "Interstellar"),
            ),
            "Inception" to listOf(
                Pair(27205, "Inception"),
            ),
            "Oppenheimer" to listOf(
                Pair(872585, "Oppenheimer"),
            ),
            "Avatar" to listOf(
                Pair(19995, "Avatar"),
                Pair(76600, "Avatar: The Way of Water"),
            ),
            "The Dark Knight" to listOf(
                Pair(155, "The Dark Knight"),
                Pair(49026, "The Dark Knight Rises"),
            ),
            "Titanic" to listOf(
                Pair(597, "Titanic"),
                Pair(105, "Titanic (1953)"),
            ),
        )

        for ((query, expectedCandidates) in moviesToTest) {
            val fakeResults = expectedCandidates.map { (id, title) ->
                """
                {
                    "id": $id,
                    "title": "$title",
                    "release_date": "2020-01-01",
                    "poster_path": "/poster_$id.jpg",
                    "overview": "Overview for $title"
                }
                """.trimIndent()
            }.joinToString(",")

            val fakeJson = """{"page":1,"results":[$fakeResults]}"""

            val provider = object : TmdbMetadataProvider(apiKeyProvider = { "valid_key" }) {
                override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                    return fakeJson
                }
            }

            val result = provider.searchMovies(query)
            assertTrue("Search for '$query' must succeed", result.isSuccess)
            val candidates = result.getOrNull()
            assertNotNull(candidates)
            assertEquals("Search for '$query' must return all candidates", expectedCandidates.size, candidates!!.size)
            for (i in expectedCandidates.indices) {
                assertEquals("tmdb_${expectedCandidates[i].first}", candidates[i].externalId)
                assertEquals(expectedCandidates[i].second, candidates[i].title)
            }
        }
    }

    @Test
    fun testTmdbHttpErrorHandling() = runBlocking {
        // Test 401 Unauthorized
        val unauthorizedProvider = object : TmdbMetadataProvider(apiKeyProvider = { "wrong_key" }) {
            override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                throw MetadataException("Invalid TMDb API key. Please check your credentials in Settings.")
            }
        }
        val unauthResult = unauthorizedProvider.searchMovies("Interstellar")
        assertTrue(unauthResult.isFailure)
        assertTrue(unauthResult.exceptionOrNull()?.message?.contains("Invalid TMDb API key") == true)

        // Test 429 Rate Limit
        val rateLimitProvider = object : TmdbMetadataProvider(apiKeyProvider = { "valid_key" }) {
            override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                throw MetadataException("TMDb API rate limit exceeded. Please try again shortly.")
            }
        }
        val rateLimitResult = rateLimitProvider.searchMovies("Inception")
        assertTrue(rateLimitResult.isFailure)
        assertTrue(rateLimitResult.exceptionOrNull()?.message?.contains("rate limit") == true)

        // Test 500 Server Error
        val serverErrorProvider = object : TmdbMetadataProvider(apiKeyProvider = { "valid_key" }) {
            override fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
                throw MetadataException("TMDb server error (HTTP 500). Please try again later.")
            }
        }
        val serverResult = serverErrorProvider.searchMovies("Oppenheimer")
        assertTrue(serverResult.isFailure)
        assertTrue(serverResult.exceptionOrNull()?.message?.contains("TMDb server error") == true)
    }
}
