package com.example.moviewather.data.backup

import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.local.entity.MovieCategoryCrossRef
import com.example.moviewather.data.util.MovieDuplicateDetector
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Offline serializer, deserializer, and validator for Movie Watcher backups.
 */
object BackupManager {

    const val CURRENT_BACKUP_VERSION = 1

    /**
     * Serializes movies, categories, and junction relationships into a formatted JSON backup string.
     */
    fun serializeBackup(
        movies: List<Movie>,
        categories: List<Category>,
        crossRefs: List<MovieCategoryCrossRef>,
    ): String {
        val root = JSONObject()
        root.put("version", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val moviesArray = JSONArray()
        for (movie in movies) {
            val movieObj = JSONObject()
            movieObj.put("id", movie.id)
            movieObj.put("title", movie.title)
            movieObj.put("isWatched", movie.isWatched)
            movieObj.put("isFavorite", movie.isFavorite)
            movieObj.put("dateAdded", movie.dateAdded)
            if (movie.dateWatched != null) {
                movieObj.put("dateWatched", movie.dateWatched)
            } else {
                movieObj.put("dateWatched", JSONObject.NULL)
            }
            movieObj.put("notes", movie.notes)
            if (movie.releaseYear != null) movieObj.put("releaseYear", movie.releaseYear) else movieObj.put("releaseYear", JSONObject.NULL)
            if (movie.posterUrl != null) movieObj.put("posterUrl", movie.posterUrl) else movieObj.put("posterUrl", JSONObject.NULL)
            if (movie.overview != null) movieObj.put("overview", movie.overview) else movieObj.put("overview", JSONObject.NULL)
            if (movie.genres != null) movieObj.put("genres", movie.genres) else movieObj.put("genres", JSONObject.NULL)
            if (movie.director != null) movieObj.put("director", movie.director) else movieObj.put("director", JSONObject.NULL)
            if (movie.mainCast != null) movieObj.put("mainCast", movie.mainCast) else movieObj.put("mainCast", JSONObject.NULL)
            if (movie.runtime != null) movieObj.put("runtime", movie.runtime) else movieObj.put("runtime", JSONObject.NULL)
            if (movie.externalId != null) movieObj.put("externalId", movie.externalId) else movieObj.put("externalId", JSONObject.NULL)
            if (movie.metadataProvider != null) movieObj.put("metadataProvider", movie.metadataProvider) else movieObj.put("metadataProvider", JSONObject.NULL)
            if (movie.metadataLastUpdated != null) movieObj.put("metadataLastUpdated", movie.metadataLastUpdated) else movieObj.put("metadataLastUpdated", JSONObject.NULL)
            moviesArray.put(movieObj)
        }
        root.put("movies", moviesArray)

        val categoriesArray = JSONArray()
        for (category in categories) {
            val catObj = JSONObject()
            catObj.put("id", category.id)
            catObj.put("name", category.name)
            catObj.put("dateCreated", category.dateCreated)
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        val crossRefArray = JSONArray()
        for (ref in crossRefs) {
            val refObj = JSONObject()
            refObj.put("movieId", ref.movieId)
            refObj.put("categoryId", ref.categoryId)
            crossRefArray.put(refObj)
        }
        root.put("movieCategories", crossRefArray)

        return root.toString(2)
    }

    /**
     * Parses and strictly validates a JSON backup string.
     */
    fun parseAndValidate(jsonString: String): BackupValidationResult {
        if (jsonString.isBlank()) {
            return BackupValidationResult.Error("The selected file is empty.")
        }

        val root = try {
            JSONObject(jsonString)
        } catch (_: Exception) {
            return BackupValidationResult.Error("This file doesn't appear to be a valid Movie Watcher backup.")
        }

        if (!root.has("version")) {
            return BackupValidationResult.Error("Missing backup format version.")
        }

        val version = root.optInt("version", -1)
        if (version < 1 || version > CURRENT_BACKUP_VERSION) {
            return BackupValidationResult.Error("Unsupported backup version ($version). Please update Movie Watcher.")
        }

        if (!root.has("movies")) {
            return BackupValidationResult.Error("Backup file is missing movies data.")
        }

        val moviesList = mutableListOf<MovieBackupDto>()
        val moviesArray = root.optJSONArray("movies")
            ?: return BackupValidationResult.Error("Invalid movies structure in backup file.")

        for (i in 0 until moviesArray.length()) {
            val item = moviesArray.optJSONObject(i)
                ?: return BackupValidationResult.Error("Corrupted movie entry at index $i.")

            val title = item.optString("title", "").trim()
            if (title.isEmpty()) {
                return BackupValidationResult.Error("A movie entry at index $i has an empty title.")
            }

            val id = item.optLong("id", i.toLong() + 1)
            val isWatched = item.optBoolean("isWatched", false)
            val isFavorite = item.optBoolean("isFavorite", false)
            val dateAdded = item.optLong("dateAdded", System.currentTimeMillis())
            val dateWatched = if (item.isNull("dateWatched")) null else item.optLong("dateWatched")
            val notes = item.optString("notes", "")

            val releaseYear = if (item.isNull("releaseYear") || !item.has("releaseYear")) null else item.optInt("releaseYear")
            val posterUrl = if (item.isNull("posterUrl") || !item.has("posterUrl")) null else item.optString("posterUrl").ifBlank { null }
            val overview = if (item.isNull("overview") || !item.has("overview")) null else item.optString("overview").ifBlank { null }
            val genres = if (item.isNull("genres") || !item.has("genres")) null else item.optString("genres").ifBlank { null }
            val director = if (item.isNull("director") || !item.has("director")) null else item.optString("director").ifBlank { null }
            val mainCast = if (item.isNull("mainCast") || !item.has("mainCast")) null else item.optString("mainCast").ifBlank { null }
            val runtime = if (item.isNull("runtime") || !item.has("runtime")) null else item.optInt("runtime")
            val externalId = if (item.isNull("externalId") || !item.has("externalId")) null else item.optString("externalId").ifBlank { null }
            val metadataProvider = if (item.isNull("metadataProvider") || !item.has("metadataProvider")) null else item.optString("metadataProvider").ifBlank { null }
            val metadataLastUpdated = if (item.isNull("metadataLastUpdated") || !item.has("metadataLastUpdated")) null else item.optLong("metadataLastUpdated")

            moviesList.add(
                MovieBackupDto(
                    id = id,
                    title = title,
                    isWatched = isWatched,
                    isFavorite = isFavorite,
                    dateAdded = dateAdded,
                    dateWatched = dateWatched,
                    notes = notes,
                    releaseYear = releaseYear,
                    posterUrl = posterUrl,
                    overview = overview,
                    genres = genres,
                    director = director,
                    mainCast = mainCast,
                    runtime = runtime,
                    externalId = externalId,
                    metadataProvider = metadataProvider,
                    metadataLastUpdated = metadataLastUpdated,
                )
            )
        }

        val categoriesList = mutableListOf<CategoryBackupDto>()
        val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until categoriesArray.length()) {
            val item = categoriesArray.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            if (name.isEmpty()) continue
            val id = item.optLong("id", i.toLong() + 1)
            val dateCreated = item.optLong("dateCreated", System.currentTimeMillis())
            categoriesList.add(CategoryBackupDto(id = id, name = name, dateCreated = dateCreated))
        }

        val crossRefList = mutableListOf<MovieCategoryBackupDto>()
        val crossRefArray = root.optJSONArray("movieCategories") ?: JSONArray()
        for (i in 0 until crossRefArray.length()) {
            val item = crossRefArray.optJSONObject(i) ?: continue
            val movieId = item.optLong("movieId", -1L)
            val categoryId = item.optLong("categoryId", -1L)
            if (movieId != -1L && categoryId != -1L) {
                crossRefList.add(MovieCategoryBackupDto(movieId = movieId, categoryId = categoryId))
            }
        }

        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

        return BackupValidationResult.Success(
            MovieWatcherBackup(
                version = version,
                exportedAt = exportedAt,
                movies = moviesList,
                categories = categoriesList,
                movieCategories = crossRefList,
            )
        )
    }

    /**
     * Inspects the backup against the user's current database state to build an import preview.
     */
    fun createImportPreview(
        backup: MovieWatcherBackup,
        existingMovies: List<Movie>,
        existingCategories: List<Category>,
    ): ImportPreviewData {
        val existingNormalizedTitles = existingMovies.map { MovieDuplicateDetector.normalize(it.title) }.toSet()
        val duplicateCount = backup.movies.count {
            MovieDuplicateDetector.normalize(it.title) in existingNormalizedTitles
        }
        val newMoviesCount = backup.movies.size - duplicateCount

        val existingNormalizedCategories = existingCategories.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val existingCatCount = backup.categories.count {
            it.name.trim().lowercase(Locale.ROOT) in existingNormalizedCategories
        }
        val newCatCount = backup.categories.size - existingCatCount

        return ImportPreviewData(
            backup = backup,
            totalMoviesInBackup = backup.movies.size,
            totalCategoriesInBackup = backup.categories.size,
            newMoviesCount = newMoviesCount,
            duplicateMoviesCount = duplicateCount,
            newCategoriesCount = newCatCount,
            existingCategoriesCount = existingCatCount,
        )
    }
}
