package com.example.moviewather.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real-world metadata provider communicating with The Movie Database (TMDb) v3 API.
 *
 * Keeps API keys completely out of source code:
 * Keys are passed in at runtime from BuildConfig or user preferences.
 */
open class TmdbMetadataProvider(
    private val apiKeyProvider: suspend () -> String,
) : MovieMetadataProvider {

    override val providerName: String = "The Movie Database (TMDb)"

    private val baseUrl = "https://api.themoviedb.org/3"
    private val imageBaseUrl = "https://image.tmdb.org/t/p/w500"

    open override suspend fun searchMovies(query: String): Result<List<MovieMetadataResult>> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(MissingApiKeyException("TMDb API key is not configured."))
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val isBearerToken = apiKey.startsWith("ey") || apiKey.length > 40
            val urlString = if (isBearerToken) {
                "$baseUrl/search/movie?query=$encodedQuery&include_adult=false"
            } else {
                "$baseUrl/search/movie?api_key=$apiKey&query=$encodedQuery&include_adult=false"
            }
            val responseJson = executeGet(urlString, apiKey, isBearerToken)
            val root = JSONObject(responseJson)
            val resultsArray = root.optJSONArray("results") ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<MovieMetadataResult>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val id = item.optInt("id", -1)
                if (id <= 0) continue

                val title = item.optString("title", "").trim()
                if (title.isEmpty()) continue

                val releaseDate = item.optString("release_date", "")
                val releaseYear = releaseDate.take(4).toIntOrNull()

                val posterPath = item.optString("poster_path", "").trim()
                val posterUrl = if (posterPath.isNotBlank() && posterPath != "null") "$imageBaseUrl$posterPath" else null

                val overview = item.optString("overview", "").trim().ifBlank { null }

                list.add(
                    MovieMetadataResult(
                        externalId = "tmdb_$id",
                        title = title,
                        releaseYear = releaseYear,
                        posterUrl = posterUrl,
                        overview = overview,
                        genres = emptyList(),
                        director = null,
                        mainCast = emptyList(),
                        runtime = null,
                        provider = providerName,
                    )
                )
            }

            Result.success(list)
        } catch (e: MetadataException) {
            Result.failure(e)
        } catch (e: Exception) {
            val friendlyMsg = when (e) {
                is java.net.UnknownHostException, is java.net.ConnectException ->
                    "Unable to reach TMDb. Please check your internet connection."
                is java.net.SocketTimeoutException ->
                    "Connection to TMDb timed out. Please try again."
                else ->
                    "Failed to fetch movie information from TMDb: ${e.localizedMessage ?: e.message}"
            }
            Result.failure(MetadataException(friendlyMsg, e))
        }
    }

    open override suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(MissingApiKeyException("TMDb API key is not configured."))
        }

        val rawId = externalId.removePrefix("tmdb_").toIntOrNull()
            ?: return@withContext Result.failure(MetadataException("Invalid TMDb ID format ($externalId)."))

        try {
            val isBearerToken = apiKey.startsWith("ey") || apiKey.length > 40
            val urlString = if (isBearerToken) {
                "$baseUrl/movie/$rawId?append_to_response=credits"
            } else {
                "$baseUrl/movie/$rawId?api_key=$apiKey&append_to_response=credits"
            }
            val responseJson = executeGet(urlString, apiKey, isBearerToken)
            val root = JSONObject(responseJson)

            val title = root.optString("title", "").trim()
            val releaseDate = root.optString("release_date", "")
            val releaseYear = releaseDate.take(4).toIntOrNull()

            val posterPath = root.optString("poster_path", "").trim()
            val posterUrl = if (posterPath.isNotBlank() && posterPath != "null") "$imageBaseUrl$posterPath" else null

            val overview = root.optString("overview", "").trim().ifBlank { null }
            val runtime = if (root.has("runtime") && !root.isNull("runtime")) root.optInt("runtime") else null

            val genresList = mutableListOf<String>()
            val genresArray = root.optJSONArray("genres")
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    val g = genresArray.optJSONObject(i)?.optString("name", "")
                    if (!g.isNullOrBlank()) genresList.add(g)
                }
            }

            var director: String? = null
            val castList = mutableListOf<String>()

            val credits = root.optJSONObject("credits")
            if (credits != null) {
                val crewArray = credits.optJSONArray("crew")
                if (crewArray != null) {
                    for (i in 0 until crewArray.length()) {
                        val member = crewArray.optJSONObject(i) ?: continue
                        if (member.optString("job") == "Director") {
                            director = member.optString("name", "").ifBlank { null }
                            break
                        }
                    }
                }

                val castArray = credits.optJSONArray("cast")
                if (castArray != null) {
                    for (i in 0 until minOf(castArray.length(), 4)) {
                        val actor = castArray.optJSONObject(i)?.optString("name", "")
                        if (!actor.isNullOrBlank()) castList.add(actor)
                    }
                }
            }

            Result.success(
                MovieMetadataResult(
                    externalId = externalId,
                    title = title,
                    releaseYear = releaseYear,
                    posterUrl = posterUrl,
                    overview = overview,
                    genres = genresList,
                    director = director,
                    mainCast = castList,
                    runtime = runtime,
                    provider = providerName,
                )
            )
        } catch (e: MetadataException) {
            Result.failure(e)
        } catch (e: Exception) {
            val friendlyMsg = when (e) {
                is java.net.UnknownHostException, is java.net.ConnectException ->
                    "Unable to reach TMDb. Please check your internet connection."
                is java.net.SocketTimeoutException ->
                    "Connection to TMDb timed out. Please try again."
                else ->
                    "Failed to fetch movie details from TMDb: ${e.localizedMessage ?: e.message}"
            }
            Result.failure(MetadataException(friendlyMsg, e))
        }
    }

    protected open fun executeGet(urlString: String, apiKey: String, isBearerToken: Boolean): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/json")
        if (isBearerToken) {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val responseCode = conn.responseCode
        if (responseCode == 401) {
            throw MetadataException("Invalid TMDb API key. Please check your credentials in Settings.")
        }
        if (responseCode == 403) {
            throw MetadataException("Access denied by TMDb. Please verify your API key permissions.")
        }
        if (responseCode == 404) {
            throw MetadataException("Movie not found on TMDb.")
        }
        if (responseCode == 429) {
            throw MetadataException("TMDb API rate limit exceeded. Please try again shortly.")
        }
        if (responseCode >= 500) {
            throw MetadataException("TMDb server error (HTTP $responseCode). Please try again later.")
        }
        if (responseCode !in 200..299) {
            throw MetadataException("TMDb returned error HTTP $responseCode.")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        return sb.toString()
    }
}
