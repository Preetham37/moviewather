package com.example.moviewather.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.local.entity.MovieCategoryCrossRef
import com.example.moviewather.data.local.model.CategoryWithCount
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Categories and the Movie-Category many-to-many relationship.
 */
@Dao
interface CategoryDao {

    // ── Category CRUD ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun getCategoryById(id: Long): Flow<Category?>

    @Query("""
        SELECT c.*, COUNT(ref.movieId) AS movieCount
        FROM categories c
        LEFT JOIN movie_category_cross_ref ref ON c.id = ref.categoryId
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>

    // ── Junction (MovieCategoryCrossRef) Operations ───────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: MovieCategoryCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: MovieCategoryCrossRef)

    @Query("DELETE FROM movie_category_cross_ref WHERE movieId = :movieId AND categoryId = :categoryId")
    suspend fun removeMovieFromCategory(movieId: Long, categoryId: Long)

    @Query("DELETE FROM movie_category_cross_ref WHERE movieId = :movieId")
    suspend fun clearCategoriesForMovie(movieId: Long)

    @Query("""
        SELECT m.* FROM movies m
        INNER JOIN movie_category_cross_ref ref ON m.id = ref.movieId
        WHERE ref.categoryId = :categoryId
        ORDER BY m.dateAdded DESC
    """)
    fun getMoviesForCategory(categoryId: Long): Flow<List<Movie>>

    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN movie_category_cross_ref ref ON c.id = ref.categoryId
        WHERE ref.movieId = :movieId
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun getCategoriesForMovie(movieId: Long): Flow<List<Category>>

    @Query("SELECT categoryId FROM movie_category_cross_ref WHERE movieId = :movieId")
    fun getCategoryIdsForMovie(movieId: Long): Flow<List<Long>>

    @Transaction
    suspend fun setMovieCategories(movieId: Long, categoryIds: Set<Long>) {
        clearCategoriesForMovie(movieId)
        for (categoryId in categoryIds) {
            insertCrossRef(MovieCategoryCrossRef(movieId = movieId, categoryId = categoryId))
        }
    }

    /** Synchronous snapshot of all categories for backup generation and transaction imports. */
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<Category>

    /** Synchronous snapshot of all cross-references for backup generation. */
    @Query("SELECT * FROM movie_category_cross_ref")
    suspend fun getAllCrossRefsList(): List<MovieCategoryCrossRef>

    /** Permanently deletes all rows from the categories table. */
    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    /** Permanently deletes all rows from the movie_category_cross_ref junction table. */
    @Query("DELETE FROM movie_category_cross_ref")
    suspend fun clearAllCrossRefs()
}
