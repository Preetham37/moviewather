package com.example.moviewather.data.backup

/**
 * Strategy chosen by the user when duplicates are detected during backup import.
 */
enum class DuplicateStrategy(val label: String) {
    SKIP("Skip existing movies"),
    REPLACE("Replace existing records"),
    IMPORT_AS_NEW("Import as new records"),
}

/**
 * In-memory representation of an exported Movie Watcher backup.
 */
data class MovieWatcherBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val movies: List<MovieBackupDto>,
    val categories: List<CategoryBackupDto>,
    val movieCategories: List<MovieCategoryBackupDto>,
)

data class MovieBackupDto(
    val id: Long,
    val title: String,
    val isWatched: Boolean,
    val isFavorite: Boolean,
    val dateAdded: Long,
    val dateWatched: Long?,
    val notes: String,
    val releaseYear: Int? = null,
    val posterUrl: String? = null,
    val overview: String? = null,
    val genres: String? = null,
    val director: String? = null,
    val mainCast: String? = null,
    val runtime: Int? = null,
    val externalId: String? = null,
    val metadataProvider: String? = null,
    val metadataLastUpdated: Long? = null,
)

data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val dateCreated: Long,
)

data class MovieCategoryBackupDto(
    val movieId: Long,
    val categoryId: Long,
)

/**
 * Inspection data generated prior to confirming an import.
 */
data class ImportPreviewData(
    val backup: MovieWatcherBackup,
    val totalMoviesInBackup: Int,
    val totalCategoriesInBackup: Int,
    val newMoviesCount: Int,
    val duplicateMoviesCount: Int,
    val newCategoriesCount: Int,
    val existingCategoriesCount: Int,
)

/**
 * Summary stats generated after successfully executing an import transaction.
 */
data class ImportResultSummary(
    val newMoviesAdded: Int,
    val moviesReplaced: Int,
    val existingMoviesSkipped: Int,
    val categoriesRestored: Int,
    val relationshipsRestored: Int,
)

/**
 * Result of validating an uploaded backup file.
 */
sealed interface BackupValidationResult {
    data class Success(val backup: MovieWatcherBackup) : BackupValidationResult
    data class Error(val message: String) : BackupValidationResult
}
