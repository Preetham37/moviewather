package com.example.moviewather.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a movie in the user's collection.
 *
 * Fields kept deliberately minimal for Section 2 – only what's needed
 * for the watchlist / watched workflow is included now.
 *
 * Future fields (release year, poster URL, genre, director, cast,
 * runtime, external movie ID, categories) should be added as new
 * nullable columns with default values so existing rows are not broken.
 */
@Entity(tableName = "movies")
data class Movie(
    /** Auto-generated primary key. Do NOT supply a value when inserting a new movie. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The movie title as entered by the user. */
    val title: String,

    /** True once the user has marked the movie as watched. */
    val isWatched: Boolean = false,

    /** True when the user has starred the movie as a favourite. */
    val isFavorite: Boolean = false,

    /**
     * Unix epoch milliseconds recorded when the movie was first saved.
     * Use [System.currentTimeMillis] when creating a new [Movie].
     */
    val dateAdded: Long = System.currentTimeMillis(),

    /**
     * Unix epoch milliseconds recorded when the user marks the movie watched.
     * Null while the movie is still on the watchlist.
     */
    val dateWatched: Long? = null,

    /** Optional personal notes the user can attach to the movie. */
    val notes: String = "",

    /** Optional release year (e.g. 1984, 2021) for distinguishing movies with identical titles. */
    val releaseYear: Int? = null,

    /** Optional poster URL (remote image URL or cached reference). */
    val posterUrl: String? = null,

    /** Optional short plot summary or overview. */
    val overview: String? = null,

    /** Optional comma-separated genres (e.g. "Sci-Fi, Adventure"). */
    val genres: String? = null,

    /** Optional director name (e.g. "Denis Villeneuve"). */
    val director: String? = null,

    /** Optional primary cast names (e.g. "Timothée Chalamet, Zendaya"). */
    val mainCast: String? = null,

    /** Optional runtime in minutes (e.g. 155). */
    val runtime: Int? = null,

    /** Unique identifier from external metadata provider (e.g. "tmdb_438631"). */
    val externalId: String? = null,

    /** Name of the metadata provider that identified this movie (e.g. "TMDb", "Demo"). */
    val metadataProvider: String? = null,

    /** Epoch timestamp when metadata was downloaded or last refreshed. */
    val metadataLastUpdated: Long? = null,
)
