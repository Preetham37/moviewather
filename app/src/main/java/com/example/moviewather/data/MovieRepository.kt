package com.example.moviewather.data

import androidx.room.withTransaction
import com.example.moviewather.data.backup.BackupManager
import com.example.moviewather.data.backup.DuplicateStrategy
import com.example.moviewather.data.backup.ImportResultSummary
import com.example.moviewather.data.backup.MovieWatcherBackup
import com.example.moviewather.data.local.AppDatabase
import com.example.moviewather.data.local.dao.CategoryDao
import com.example.moviewather.data.local.dao.MovieDao
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.local.entity.MovieCategoryCrossRef
import com.example.moviewather.data.local.model.CategoryWithCount
import com.example.moviewather.data.util.MovieDuplicateDetector
import com.example.moviewather.data.metadata.MetadataException
import com.example.moviewather.data.metadata.MovieMetadataProvider
import com.example.moviewather.data.metadata.MovieMetadataResult
import kotlinx.coroutines.flow.Flow
import java.util.Locale

/**
 * Repository – the single access point to movie and category data for the rest of the app.
 *
 * Encapsulates Room DAOs, atomic transactional database operations, and metadata lookup.
 */
class MovieRepository(
    private val database: AppDatabase,
    private val movieDao: MovieDao,
    private val categoryDao: CategoryDao,
    private val metadataProvider: MovieMetadataProvider? = null,
) {

    // ─── Movie Reactive streams ───────────────────────────────────────────────

    /** Live list of all movies, newest first. */
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()

    /** Live list of movies the user still wants to watch. */
    val unwatchedMovies: Flow<List<Movie>> = movieDao.getUnwatchedMovies()

    /** Live list of movies the user has already watched. */
    val watchedMovies: Flow<List<Movie>> = movieDao.getWatchedMovies()

    // ─── Movie Write operations ───────────────────────────────────────────────

    /** Persists a new [movie]. ID is auto-generated and returned. */
    suspend fun insert(movie: Movie): Long = movieDao.insert(movie)

    /** Overwrites the stored row for [movie] (matched by its ID). */
    suspend fun update(movie: Movie) = movieDao.update(movie)

    /** Permanently removes [movie] from the database. */
    suspend fun delete(movie: Movie) = movieDao.delete(movie)

    /**
     * Returns a Flow that emits the movie with the given [id], or null
     * if no such movie exists.
     */
    fun getMovieById(id: Long): Flow<Movie?> = movieDao.getMovieById(id)

    // ─── Metadata Provider Operations ─────────────────────────────────────────

    /**
     * Searches for candidate movies from external metadata providers.
     */
    suspend fun searchMetadata(query: String): Result<List<MovieMetadataResult>> {
        val provider = metadataProvider ?: return Result.failure(MetadataException("No metadata provider configured."))
        return provider.searchMovies(query)
    }

    /**
     * Fetches detailed metadata for a specific external ID.
     */
    suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult> {
        val provider = metadataProvider ?: return Result.failure(MetadataException("No metadata provider configured."))
        return provider.getMovieDetails(externalId)
    }

    /**
     * Refreshes metadata for an existing identified movie and persists updates to Room.
     */
    suspend fun refreshMovieMetadata(movie: Movie): Result<Movie> {
        val externalId = movie.externalId
        if (externalId.isNullOrBlank()) {
            return Result.failure(MetadataException("Movie has no external identifier to refresh."))
        }
        val provider = metadataProvider ?: return Result.failure(MetadataException("No metadata provider configured."))
        val detailsResult = provider.getMovieDetails(externalId)
        if (detailsResult.isFailure) {
            return Result.failure(detailsResult.exceptionOrNull() ?: MetadataException("Failed to refresh movie information."))
        }
        val details = detailsResult.getOrThrow()
        val updated = movie.copy(
            releaseYear = details.releaseYear ?: movie.releaseYear,
            posterUrl = details.posterUrl ?: movie.posterUrl,
            overview = details.overview ?: movie.overview,
            genres = if (details.genres.isNotEmpty()) details.genresDisplay else movie.genres,
            director = details.director ?: movie.director,
            mainCast = if (details.mainCast.isNotEmpty()) details.mainCastDisplay else movie.mainCast,
            runtime = details.runtime ?: movie.runtime,
            metadataProvider = details.provider,
            metadataLastUpdated = System.currentTimeMillis(),
        )
        movieDao.update(updated)
        return Result.success(updated)
    }

    // ─── Category Reactive streams ────────────────────────────────────────────

    /** Live list of all categories ordered alphabetically by name. */
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    /** Live list of all categories alongside their real-time movie count. */
    val categoriesWithCount: Flow<List<CategoryWithCount>> = categoryDao.getCategoriesWithCount()

    // ─── Category Operations ──────────────────────────────────────────────────

    fun getCategoryById(id: Long): Flow<Category?> = categoryDao.getCategoryById(id)

    fun getMoviesForCategory(categoryId: Long): Flow<List<Movie>> =
        categoryDao.getMoviesForCategory(categoryId)

    fun getCategoriesForMovie(movieId: Long): Flow<List<Category>> =
        categoryDao.getCategoriesForMovie(movieId)

    fun getCategoryIdsForMovie(movieId: Long): Flow<List<Long>> =
        categoryDao.getCategoryIdsForMovie(movieId)

    suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category)

    suspend fun addMovieToCategory(movieId: Long, categoryId: Long) =
        categoryDao.insertCrossRef(MovieCategoryCrossRef(movieId, categoryId))

    suspend fun removeMovieFromCategory(movieId: Long, categoryId: Long) =
        categoryDao.removeMovieFromCategory(movieId, categoryId)

    suspend fun setMovieCategories(movieId: Long, categoryIds: Set<Long>) =
        categoryDao.setMovieCategories(movieId, categoryIds)

    // ─── Export, Import & Clear All Data ──────────────────────────────────────

    /**
     * Generates a complete JSON backup string representing the current state of all
     * movies, categories, and junction relationships.
     */
    suspend fun generateBackupJson(): String {
        val movies = movieDao.getAllMoviesList()
        val categories = categoryDao.getAllCategoriesList()
        val crossRefs = categoryDao.getAllCrossRefsList()
        return BackupManager.serializeBackup(movies, categories, crossRefs)
    }

    /**
     * Atomically executes an import within a database transaction.
     */
    suspend fun executeImport(
        backup: MovieWatcherBackup,
        strategy: DuplicateStrategy,
    ): ImportResultSummary = database.withTransaction {
        var newMoviesAdded = 0
        var moviesReplaced = 0
        var existingMoviesSkipped = 0
        var categoriesRestored = 0
        var relationshipsRestored = 0

        // 1. Process categories: merge by normalized name or create new
        val existingCategories = categoryDao.getAllCategoriesList()
        val existingCatMap = existingCategories.associateBy { it.name.trim().lowercase(Locale.ROOT) }
        val categoryIdMap = mutableMapOf<Long, Long>() // backupCategoryId -> actualCategoryId

        for (backupCat in backup.categories) {
            val normalizedName = backupCat.name.trim().lowercase(Locale.ROOT)
            val existing = existingCatMap[normalizedName]
            if (existing != null) {
                categoryIdMap[backupCat.id] = existing.id
            } else {
                val newId = categoryDao.insertCategory(
                    Category(
                        name = backupCat.name.trim(),
                        dateCreated = backupCat.dateCreated,
                    )
                )
                categoryIdMap[backupCat.id] = newId
                categoriesRestored++
            }
        }

        // 2. Process movies: duplicate handling according to chosen strategy
        val existingMovies = movieDao.getAllMoviesList()
        val existingMoviesByTitle = existingMovies.associateBy {
            MovieDuplicateDetector.normalize(it.title)
        }
        val movieIdMap = mutableMapOf<Long, Long>() // backupMovieId -> actualMovieId

        for (movieBackup in backup.movies) {
            val normalizedTitle = MovieDuplicateDetector.normalize(movieBackup.title)
            val existingMovie = existingMoviesByTitle[normalizedTitle]

            if (existingMovie != null) {
                when (strategy) {
                    DuplicateStrategy.SKIP -> {
                        existingMoviesSkipped++
                        movieIdMap[movieBackup.id] = existingMovie.id
                    }
                    DuplicateStrategy.REPLACE -> {
                        val updated = existingMovie.copy(
                            isWatched = movieBackup.isWatched,
                            isFavorite = movieBackup.isFavorite,
                            dateAdded = movieBackup.dateAdded,
                            dateWatched = movieBackup.dateWatched,
                            notes = movieBackup.notes,
                            releaseYear = movieBackup.releaseYear ?: existingMovie.releaseYear,
                            posterUrl = movieBackup.posterUrl ?: existingMovie.posterUrl,
                            overview = movieBackup.overview ?: existingMovie.overview,
                            genres = movieBackup.genres ?: existingMovie.genres,
                            director = movieBackup.director ?: existingMovie.director,
                            mainCast = movieBackup.mainCast ?: existingMovie.mainCast,
                            runtime = movieBackup.runtime ?: existingMovie.runtime,
                            externalId = movieBackup.externalId ?: existingMovie.externalId,
                            metadataProvider = movieBackup.metadataProvider ?: existingMovie.metadataProvider,
                            metadataLastUpdated = movieBackup.metadataLastUpdated ?: existingMovie.metadataLastUpdated,
                        )
                        movieDao.update(updated)
                        moviesReplaced++
                        movieIdMap[movieBackup.id] = existingMovie.id
                    }
                    DuplicateStrategy.IMPORT_AS_NEW -> {
                        val newId = movieDao.insert(
                            Movie(
                                title = movieBackup.title,
                                isWatched = movieBackup.isWatched,
                                isFavorite = movieBackup.isFavorite,
                                dateAdded = movieBackup.dateAdded,
                                dateWatched = movieBackup.dateWatched,
                                notes = movieBackup.notes,
                                releaseYear = movieBackup.releaseYear,
                                posterUrl = movieBackup.posterUrl,
                                overview = movieBackup.overview,
                                genres = movieBackup.genres,
                                director = movieBackup.director,
                                mainCast = movieBackup.mainCast,
                                runtime = movieBackup.runtime,
                                externalId = movieBackup.externalId,
                                metadataProvider = movieBackup.metadataProvider,
                                metadataLastUpdated = movieBackup.metadataLastUpdated,
                            )
                        )
                        newMoviesAdded++
                        movieIdMap[movieBackup.id] = newId
                    }
                }
            } else {
                val newId = movieDao.insert(
                    Movie(
                        title = movieBackup.title,
                        isWatched = movieBackup.isWatched,
                        isFavorite = movieBackup.isFavorite,
                        dateAdded = movieBackup.dateAdded,
                        dateWatched = movieBackup.dateWatched,
                        notes = movieBackup.notes,
                        releaseYear = movieBackup.releaseYear,
                        posterUrl = movieBackup.posterUrl,
                        overview = movieBackup.overview,
                        genres = movieBackup.genres,
                        director = movieBackup.director,
                        mainCast = movieBackup.mainCast,
                        runtime = movieBackup.runtime,
                        externalId = movieBackup.externalId,
                        metadataProvider = movieBackup.metadataProvider,
                        metadataLastUpdated = movieBackup.metadataLastUpdated,
                    )
                )
                newMoviesAdded++
                movieIdMap[movieBackup.id] = newId
            }
        }

        // 3. Restore movie-category relationships
        for (ref in backup.movieCategories) {
            val actualMovieId = movieIdMap[ref.movieId]
            val actualCategoryId = categoryIdMap[ref.categoryId]
            if (actualMovieId != null && actualCategoryId != null) {
                categoryDao.insertCrossRef(
                    MovieCategoryCrossRef(
                        movieId = actualMovieId,
                        categoryId = actualCategoryId,
                    )
                )
                relationshipsRestored++
            }
        }

        ImportResultSummary(
            newMoviesAdded = newMoviesAdded,
            moviesReplaced = moviesReplaced,
            existingMoviesSkipped = existingMoviesSkipped,
            categoriesRestored = categoriesRestored,
            relationshipsRestored = relationshipsRestored,
        )
    }

    /**
     * Completely and safely purges all user movie data within an atomic transaction.
     */
    suspend fun clearAllData() = database.withTransaction {
        categoryDao.clearAllCrossRefs()
        movieDao.clearAllMovies()
        categoryDao.clearAllCategories()
    }
}
