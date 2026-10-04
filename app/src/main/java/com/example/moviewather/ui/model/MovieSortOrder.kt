package com.example.moviewather.ui.model

/**
 * Universal sort options matching the design specification.
 */
enum class MovieSortOption(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    OLDEST_ADDED("Oldest Added"),
    TITLE_A_Z("Title (A → Z)"),
    TITLE_Z_A("Title (Z → A)"),
    RELEASE_YEAR_NEW_OLD("Release Year (New → Old)"),
    RELEASE_YEAR_OLD_NEW("Release Year (Old → New)"),
}

/**
 * Filter criteria state for Watchlist and Watched screens.
 */
data class MovieFilterState(
    val favoritesOnly: Boolean = false,
    val withCategories: Boolean = false,
    val withMetadata: Boolean = false,
    val withoutMetadata: Boolean = false,
) {
    val isActive: Boolean
        get() = favoritesOnly || withCategories || withMetadata || withoutMetadata
}

// Compatibility aliases
typealias WatchlistSortOrder = MovieSortOption
typealias WatchedSortOrder = MovieSortOption
