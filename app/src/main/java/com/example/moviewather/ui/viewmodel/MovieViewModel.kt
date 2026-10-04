package com.example.moviewather.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moviewather.data.MovieRepository
import com.example.moviewather.data.backup.BackupManager
import com.example.moviewather.data.backup.DuplicateStrategy
import com.example.moviewather.data.backup.ImportPreviewData
import com.example.moviewather.data.backup.ImportResultSummary
import com.example.moviewather.data.backup.MovieWatcherBackup
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.local.model.CategoryWithCount
import com.example.moviewather.data.metadata.MovieMetadataResult
import com.example.moviewather.data.preference.AppTheme
import com.example.moviewather.data.preference.UserPreferencesRepository
import com.example.moviewather.data.util.DuplicateCheckResult
import com.example.moviewather.data.util.MovieDuplicateDetector
import com.example.moviewather.ui.model.WatchedSortOrder
import com.example.moviewather.ui.model.WatchlistSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing movie data, reactive search, sorting, duplicate checking,
 * categories, backup/import, and user preferences.
 */
class MovieViewModel(
    val repository: MovieRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    // ─── User Preferences State ───────────────────────────────────────────────

    val currentTheme: StateFlow<AppTheme> = preferencesRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = AppTheme.SYSTEM,
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    val tmdbApiKey: StateFlow<String> = preferencesRepository.tmdbApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = preferencesRepository.getEffectiveTmdbApiKeySync(),
        )

    fun setTmdbApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setTmdbApiKey(apiKey)
        }
    }

    // ─── Search & Sort State ──────────────────────────────────────────────────

    private val _watchlistSearchQuery = MutableStateFlow("")
    val watchlistSearchQuery: StateFlow<String> = _watchlistSearchQuery.asStateFlow()

    private val _watchlistSortOrder = MutableStateFlow(WatchlistSortOrder.RECENTLY_ADDED)
    val watchlistSortOrder: StateFlow<WatchlistSortOrder> = _watchlistSortOrder.asStateFlow()

    private val _watchedSearchQuery = MutableStateFlow("")
    val watchedSearchQuery: StateFlow<String> = _watchedSearchQuery.asStateFlow()

    private val _watchedSortOrder = MutableStateFlow(WatchedSortOrder.RECENTLY_WATCHED)
    val watchedSortOrder: StateFlow<WatchedSortOrder> = _watchedSortOrder.asStateFlow()

    // ─── Underlying Data Streams ──────────────────────────────────────────────

    val unwatchedMovies: StateFlow<List<Movie>> = repository.unwatchedMovies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    val watchedMovies: StateFlow<List<Movie>> = repository.watchedMovies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    val allMovies: StateFlow<List<Movie>> = repository.allMovies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    val categoriesWithCount: StateFlow<List<CategoryWithCount>> = repository.categoriesWithCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    // ─── Filtered and Sorted UI Streams ───────────────────────────────────────

    val filteredWatchlist: StateFlow<List<Movie>> = combine(
        repository.unwatchedMovies,
        _watchlistSearchQuery,
        _watchlistSortOrder,
    ) { movies, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            movies
        } else {
            val trimmed = query.trim()
            movies.filter { it.title.contains(trimmed, ignoreCase = true) }
        }

        when (sortOrder) {
            WatchlistSortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAdded }
            WatchlistSortOrder.OLDEST_ADDED -> filtered.sortedBy { it.dateAdded }
            WatchlistSortOrder.TITLE_A_Z -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            WatchlistSortOrder.TITLE_Z_A -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList(),
    )

    val filteredWatched: StateFlow<List<Movie>> = combine(
        repository.watchedMovies,
        _watchedSearchQuery,
        _watchedSortOrder,
    ) { movies, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            movies
        } else {
            val trimmed = query.trim()
            movies.filter { it.title.contains(trimmed, ignoreCase = true) }
        }

        when (sortOrder) {
            WatchedSortOrder.RECENTLY_WATCHED -> filtered.sortedByDescending { it.dateWatched ?: 0L }
            WatchedSortOrder.OLDEST_WATCHED -> filtered.sortedBy { it.dateWatched ?: Long.MAX_VALUE }
            WatchedSortOrder.TITLE_A_Z -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            WatchedSortOrder.TITLE_Z_A -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList(),
    )

    // ─── Search & Sort Mutators ───────────────────────────────────────────────

    fun setWatchlistSearchQuery(query: String) {
        _watchlistSearchQuery.value = query
    }

    fun setWatchlistSortOrder(sortOrder: WatchlistSortOrder) {
        _watchlistSortOrder.value = sortOrder
    }

    fun setWatchedSearchQuery(query: String) {
        _watchedSearchQuery.value = query
    }

    fun setWatchedSortOrder(sortOrder: WatchedSortOrder) {
        _watchedSortOrder.value = sortOrder
    }

    // ─── Duplicate & Similarity Checking ──────────────────────────────────────

    /**
     * Checks a candidate title against the entire collection using the Section 9 hierarchy:
     * 1. External ID match (Very strong duplicate)
     * 2. Title + Release Year match (Strong duplicate)
     * 3. Normalized title match (Possible duplicate, distinguishing distinct release years)
     * 4. Similar title warning
     */
    fun checkDuplicate(
        title: String,
        candidateYear: Int? = null,
        candidateExternalId: String? = null,
    ): DuplicateCheckResult {
        return MovieDuplicateDetector.check(
            title = title,
            existingMovies = allMovies.value,
            candidateYear = candidateYear,
            candidateExternalId = candidateExternalId,
        )
    }

    /**
     * Checks a candidate title against the collection, excluding the movie currently being edited.
     */
    fun checkDuplicateForEdit(
        title: String,
        currentMovieId: Long,
        candidateYear: Int? = null,
        candidateExternalId: String? = null,
    ): DuplicateCheckResult {
        val otherMovies = allMovies.value.filter { it.id != currentMovieId }
        return MovieDuplicateDetector.check(
            title = title,
            existingMovies = otherMovies,
            candidateYear = candidateYear,
            candidateExternalId = candidateExternalId,
        )
    }

    // ─── Movie Metadata Operations ────────────────────────────────────────────

    suspend fun searchMetadata(query: String): Result<List<MovieMetadataResult>> {
        return repository.searchMetadata(query)
    }

    suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult> {
        return repository.getMovieDetails(externalId)
    }

    fun addMovieWithMetadata(
        title: String,
        notes: String = "",
        metadata: MovieMetadataResult? = null,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val movie = if (metadata != null) {
                Movie(
                    title = trimmed,
                    notes = notes,
                    releaseYear = metadata.releaseYear,
                    posterUrl = metadata.posterUrl,
                    overview = metadata.overview,
                    genres = if (metadata.genres.isNotEmpty()) metadata.genresDisplay else null,
                    director = metadata.director,
                    mainCast = if (metadata.mainCast.isNotEmpty()) metadata.mainCastDisplay else null,
                    runtime = metadata.runtime,
                    externalId = metadata.externalId,
                    metadataProvider = metadata.provider,
                    metadataLastUpdated = System.currentTimeMillis(),
                )
            } else {
                Movie(
                    title = trimmed,
                    notes = notes,
                )
            }
            repository.insert(movie)
        }
    }

    fun refreshMovieMetadata(movie: Movie, onResult: (Result<Movie>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.refreshMovieMetadata(movie)
            onResult(res)
        }
    }

    fun updateMovieDetails(
        movie: Movie,
        newTitle: String,
        newNotes: String,
        newReleaseYear: Int?,
    ) {
        val trimmedTitle = newTitle.trim()
        if (trimmedTitle.isEmpty()) return
        viewModelScope.launch {
            repository.update(
                movie.copy(
                    title = trimmedTitle,
                    notes = newNotes.trim(),
                    releaseYear = newReleaseYear,
                )
            )
        }
    }

    // ─── Domain Actions ───────────────────────────────────────────────────────

    /**
     * Observes a single movie reactively by its [id].
     */
    fun getMovieById(id: Long): kotlinx.coroutines.flow.Flow<Movie?> {
        return repository.getMovieById(id)
    }

    fun addMovie(title: String, notes: String = "") {
        addMovieWithMetadata(title, notes, metadata = null)
    }

    fun addMultipleMovies(movies: List<Pair<String, String>>) {
        viewModelScope.launch {
            movies.forEach { (title, notes) ->
                val trimmed = title.trim()
                if (trimmed.isNotEmpty()) {
                    repository.insert(Movie(title = trimmed, notes = notes))
                }
            }
        }
    }

    fun updateMovie(movie: Movie) {
        viewModelScope.launch {
            repository.update(movie)
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.update(movie.copy(isFavorite = !movie.isFavorite))
        }
    }

    fun updateNotes(movie: Movie, notes: String) {
        viewModelScope.launch {
            repository.update(movie.copy(notes = notes.trim()))
        }
    }

    fun updateMovieTitle(movie: Movie, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.update(movie.copy(title = trimmed))
        }
    }

    fun markWatched(movie: Movie) {
        viewModelScope.launch {
            repository.update(
                movie.copy(
                    isWatched = true,
                    dateWatched = System.currentTimeMillis(),
                )
            )
        }
    }

    fun markUnwatched(movie: Movie) {
        viewModelScope.launch {
            repository.update(
                movie.copy(
                    isWatched = false,
                    dateWatched = null,
                )
            )
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.delete(movie)
        }
    }

    // ─── Category Domain Actions ──────────────────────────────────────────────

    /**
     * Finds any existing category that conflicts with [name], ignoring case and whitespace.
     */
    fun findDuplicateCategory(name: String, excludeCategoryId: Long? = null): Category? {
        val normalized = name.trim().lowercase(java.util.Locale.ROOT)
        if (normalized.isEmpty()) return null
        return allCategories.value.firstOrNull {
            (excludeCategoryId == null || it.id != excludeCategoryId) &&
            it.name.trim().lowercase(java.util.Locale.ROOT) == normalized
        }
    }

    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertCategory(Category(name = trimmed))
        }
    }

    fun updateCategoryName(category: Category, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = trimmed))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun getCategoryById(categoryId: Long): kotlinx.coroutines.flow.Flow<Category?> =
        repository.getCategoryById(categoryId)

    fun getMoviesForCategory(categoryId: Long): kotlinx.coroutines.flow.Flow<List<Movie>> =
        repository.getMoviesForCategory(categoryId)

    fun getCategoriesForMovie(movieId: Long): kotlinx.coroutines.flow.Flow<List<Category>> =
        repository.getCategoriesForMovie(movieId)

    fun getCategoryIdsForMovie(movieId: Long): kotlinx.coroutines.flow.Flow<List<Long>> =
        repository.getCategoryIdsForMovie(movieId)

    fun setMovieCategories(movieId: Long, categoryIds: Set<Long>) {
        viewModelScope.launch {
            repository.setMovieCategories(movieId, categoryIds)
        }
    }

    fun removeMovieFromCategory(movieId: Long, categoryId: Long) {
        viewModelScope.launch {
            repository.removeMovieFromCategory(movieId, categoryId)
        }
    }

    /**
     * Emits a reactive, filtered, and sorted list of movies belonging to a specific category.
     */
    fun getFilteredMoviesForCategory(
        categoryId: Long,
        queryFlow: StateFlow<String>,
        sortOrderFlow: StateFlow<WatchlistSortOrder>,
    ): kotlinx.coroutines.flow.Flow<List<Movie>> = combine(
        repository.getMoviesForCategory(categoryId),
        queryFlow,
        sortOrderFlow,
    ) { movies, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            movies
        } else {
            val trimmed = query.trim()
            movies.filter { it.title.contains(trimmed, ignoreCase = true) }
        }

        when (sortOrder) {
            WatchlistSortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAdded }
            WatchlistSortOrder.OLDEST_ADDED -> filtered.sortedBy { it.dateAdded }
            WatchlistSortOrder.TITLE_A_Z -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            WatchlistSortOrder.TITLE_Z_A -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }

    // ─── Backup, Import & Data Management ────────────────────────────────────

    suspend fun generateBackupJson(): String = repository.generateBackupJson()

    fun createImportPreview(backup: MovieWatcherBackup): ImportPreviewData {
        return BackupManager.createImportPreview(backup, allMovies.value, allCategories.value)
    }

    suspend fun executeImport(
        backup: MovieWatcherBackup,
        strategy: DuplicateStrategy,
    ): ImportResultSummary {
        return repository.executeImport(backup, strategy)
    }

    suspend fun clearAllData() {
        repository.clearAllData()
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val repository: MovieRepository,
        private val preferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
                return MovieViewModel(repository, preferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
