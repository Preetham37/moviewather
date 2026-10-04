package com.example.moviewather.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table entity representing the many-to-many relationship
 * between movies and categories.
 *
 * Foreign keys ensure cascade deletions:
 * - Deleting a category removes its cross-ref records without touching movies.
 * - Deleting a movie removes its cross-ref records without touching categories.
 */
@Entity(
    tableName = "movie_category_cross_ref",
    primaryKeys = ["movieId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Movie::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["categoryId"]),
    ],
)
data class MovieCategoryCrossRef(
    val movieId: Long,
    val categoryId: Long,
)
