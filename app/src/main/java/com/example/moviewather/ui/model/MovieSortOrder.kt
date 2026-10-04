package com.example.moviewather.ui.model

/**
 * Sorting options available for the Watchlist screen.
 */
enum class WatchlistSortOrder(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    OLDEST_ADDED("Oldest Added"),
    TITLE_A_Z("A → Z"),
    TITLE_Z_A("Z → A"),
}

/**
 * Sorting options available for the Watched screen.
 */
enum class WatchedSortOrder(val label: String) {
    RECENTLY_WATCHED("Recently Watched"),
    OLDEST_WATCHED("Oldest Watched"),
    TITLE_A_Z("A → Z"),
    TITLE_Z_A("Z → A"),
}
