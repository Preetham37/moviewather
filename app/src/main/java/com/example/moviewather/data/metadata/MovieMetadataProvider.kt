package com.example.moviewather.data.metadata

/**
 * Abstraction for external movie metadata sources.
 *
 * Implementations decouple the rest of the application (Repository, ViewModel, UI)
 * from any specific third-party API or SDK.
 */
interface MovieMetadataProvider {

    /** Identifier name of this provider (e.g. "TMDb", "Demo / Offline Test"). */
    val providerName: String

    /**
     * Searches for candidate movies matching the user's title [query].
     *
     * Returns a list of candidates so the user can choose the exact movie they intended.
     */
    suspend fun searchMovies(query: String): Result<List<MovieMetadataResult>>

    /**
     * Fetches detailed information (director, cast, runtime, overview) for a specific [externalId].
     */
    suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult>
}
