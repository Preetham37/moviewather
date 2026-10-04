package com.example.moviewather.navigation

/**
 * Sealed class representing every navigation destination in the app.
 * Each object holds its route string and, for bottom-nav destinations,
 * display metadata used by the navigation bar.
 */
sealed class Screen(val route: String) {

    // ── Bottom navigation destinations ──────────────────────────────────────
    data object Watchlist : Screen("watchlist")
    data object Categories : Screen("categories")
    data object Watched : Screen("watched")

    // ── Top-level secondary destinations ────────────────────────────────────
    data object Settings : Screen("settings")
    data object AddMovie : Screen("add_movie")
    data object MovieDetails : Screen("movie_details/{movieId}") {
        fun createRoute(movieId: Long): String = "movie_details/$movieId"
    }
    data object CategoryDetails : Screen("category_details/{categoryId}") {
        fun createRoute(categoryId: Long): String = "category_details/$categoryId"
    }
}
