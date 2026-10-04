package com.example.moviewather.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Metadata for the three main bottom-navigation tabs.
 */
enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
) {
    WATCHLIST(
        screen = Screen.Watchlist,
        label = "Watchlist",
        icon = Icons.Outlined.Bookmarks,
    ),
    CATEGORIES(
        screen = Screen.Categories,
        label = "Categories",
        icon = Icons.AutoMirrored.Outlined.List,
    ),
    WATCHED(
        screen = Screen.Watched,
        label = "Watched",
        icon = Icons.Outlined.CheckCircle,
    ),
}
