package com.example.moviewather.data.local.model

import androidx.room.Embedded
import com.example.moviewather.data.local.entity.Category

/**
 * Data model combining a [Category] entity with its live movie count.
 */
data class CategoryWithCount(
    @Embedded
    val category: Category,
    val movieCount: Int,
)
