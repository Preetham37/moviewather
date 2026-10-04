package com.example.moviewather.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moviewather.data.local.entity.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [Movie] entity.
 *
 * All queries that return live data use [Flow] so that the UI can
 * observe changes automatically whenever the database is modified.
 *
 * Suspend functions are used for write operations – they run off the
 * main thread when called from a coroutine scope.
 */
@Dao
interface MovieDao {

    // ─── Insert ───────────────────────────────────────────────────────────────

    /**
     * Inserts a new movie.  [OnConflictStrategy.IGNORE] means duplicate
     * primary-key rows are silently discarded rather than crashing.
     *
     * @return the auto-generated row ID, or -1 if the row was ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: Movie): Long

    // ─── Update ───────────────────────────────────────────────────────────────

    /** Replaces the stored row with all fields from [movie] (matched by ID). */
    @Update
    suspend fun update(movie: Movie)

    // ─── Delete ───────────────────────────────────────────────────────────────

    /** Removes [movie] from the database permanently. */
    @Delete
    suspend fun delete(movie: Movie)

    // ─── Queries ──────────────────────────────────────────────────────────────

    /**
     * Emits the full movie list ordered by [Movie.dateAdded] (newest first).
     * The Flow re-emits automatically whenever any row in the table changes.
     */
    @Query("SELECT * FROM movies ORDER BY dateAdded DESC")
    fun getAllMovies(): Flow<List<Movie>>

    /**
     * Emits only unwatched movies (watchlist), ordered newest-first.
     */
    @Query("SELECT * FROM movies WHERE isWatched = 0 ORDER BY dateAdded DESC")
    fun getUnwatchedMovies(): Flow<List<Movie>>

    /**
     * Emits only watched movies, ordered by [Movie.dateWatched] (most recently
     * watched first, with nulls pushed to the end as a safety net).
     */
    @Query("SELECT * FROM movies WHERE isWatched = 1 ORDER BY dateWatched DESC")
    fun getWatchedMovies(): Flow<List<Movie>>

    /**
     * Returns a single movie by its primary key.
     * Emits null if no matching row exists.
     */
    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun getMovieById(id: Long): Flow<Movie?>

    /** Permanently deletes all rows from the movies table. */
    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()

    /** Synchronous snapshot of all movies for backup generation and transaction imports. */
    @Query("SELECT * FROM movies")
    suspend fun getAllMoviesList(): List<Movie>

    /** Find a movie by its external provider ID (used for exact duplicate matching). */
    @Query("SELECT * FROM movies WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): Movie?
}
