package com.example.moviewather.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-defined category or custom list.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The category name (e.g. "Sci-Fi", "Must Watch", "Weekend"). */
    val name: String,

    /** Epoch timestamp in milliseconds when the category was created. */
    val dateCreated: Long = System.currentTimeMillis(),
)
