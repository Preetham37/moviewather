package com.example.moviewather.data.metadata

/**
 * Result of searching or fetching movie metadata from an external provider.
 *
 * Keeps all fields optional (except external ID, title, and provider) so that
 * the app never breaks when a provider lacks complete information.
 */
typealias MovieSearchResult = MovieMetadataResult

data class MovieMetadataResult(
    /** Unique external ID from the provider (e.g. "tmdb_438631", "demo_dune_2021"). */
    val externalId: String,

    /** Official movie title from the metadata provider. */
    val title: String,

    /** Release year (e.g. 1984, 2021). */
    val releaseYear: Int? = null,

    /** Remote poster image URL or cached reference. */
    val posterUrl: String? = null,

    /** Short plot synopsis or overview. */
    val overview: String? = null,

    /** Genre names associated with the movie (e.g. ["Sci-Fi", "Adventure"]). */
    val genres: List<String> = emptyList(),

    /** Primary director name (e.g. "Denis Villeneuve"). */
    val director: String? = null,

    /** Key cast member names (e.g. ["Timothée Chalamet", "Zendaya"]). */
    val mainCast: List<String> = emptyList(),

    /** Runtime in minutes (e.g. 155). */
    val runtime: Int? = null,

    /** Name of the metadata provider (e.g. "TMDb", "Demo"). */
    val provider: String,
) {
    /** Formatted genres string for display (e.g. "Sci-Fi · Adventure"). */
    val genresDisplay: String
        get() = genres.joinToString(" · ")

    /** Formatted cast string for display. */
    val mainCastDisplay: String
        get() = mainCast.joinToString(", ")
}

/**
 * Thrown when the device is disconnected from the internet during metadata lookup.
 */
class OfflineException(message: String = "Movie information isn't available offline.") : Exception(message)

/**
 * Thrown when no TMDb API key is configured.
 */
class MissingApiKeyException(
    message: String = "TMDb API key is not configured. Please enter your API key in Settings or local.properties."
) : Exception(message)

/**
 * Thrown when external movie metadata search fails or times out.
 */
class MetadataException(message: String, cause: Throwable? = null) : Exception(message, cause)
