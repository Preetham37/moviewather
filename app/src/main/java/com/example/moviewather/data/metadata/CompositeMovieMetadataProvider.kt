package com.example.moviewather.data.metadata

/**
 * Metadata provider that coordinates network availability,
 * TMDB API integration, and offline handling.
 *
 * Production search queries TMDB directly when configured and online.
 * Does NOT silently swallow errors or fall back to an artificial demo list.
 */
class CompositeMovieMetadataProvider(
    private val networkChecker: NetworkChecker,
    private val tmdbProvider: MovieMetadataProvider,
    private val demoProvider: MovieMetadataProvider? = null,
    private val tmdbApiKeyProvider: suspend () -> String,
) : MovieMetadataProvider {

    override val providerName: String = "The Movie Database (TMDb)"

    override suspend fun searchMovies(query: String): Result<List<MovieMetadataResult>> {
        // 1. Offline check
        if (!networkChecker.isOnline()) {
            return Result.failure(OfflineException("Movie information isn't available offline."))
        }

        // 2. API key check
        val apiKey = tmdbApiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return Result.failure(
                MissingApiKeyException(
                    "TMDb API key is not configured. Please enter your API key in Settings or local.properties."
                )
            )
        }

        // 3. Query TMDB directly and propagate the actual result/error
        return tmdbProvider.searchMovies(query)
    }

    override suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult> {
        if (!networkChecker.isOnline()) {
            return Result.failure(OfflineException("Movie information isn't available offline."))
        }

        val apiKey = tmdbApiKeyProvider().trim()
        if (externalId.startsWith("demo_") && demoProvider != null) {
            return demoProvider.getMovieDetails(externalId)
        }

        if (apiKey.isBlank()) {
            return Result.failure(
                MissingApiKeyException(
                    "TMDb API key is not configured. Please enter your API key in Settings or local.properties."
                )
            )
        }

        return tmdbProvider.getMovieDetails(externalId)
    }
}
