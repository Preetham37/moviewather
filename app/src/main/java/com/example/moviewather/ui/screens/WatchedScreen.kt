package com.example.moviewather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.ui.components.DeleteMovieDialog
import com.example.moviewather.ui.components.EditMovieDialog
import com.example.moviewather.ui.components.EmptyStateMessage
import com.example.moviewather.ui.components.MainTopBar
import com.example.moviewather.ui.components.ManageMovieCategoriesDialog
import com.example.moviewather.ui.components.MovieRow
import com.example.moviewather.ui.components.SearchBar
import com.example.moviewather.ui.components.SortFilterBottomSheet
import com.example.moviewather.ui.util.DateFormatter
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Watched screen matching Screen 8 of the reference mockup.
 *
 * Information-dense, compact rows with checked indicators, watched dates,
 * real-time search, sort & filter bottom sheet, and 3-dot overflow options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedScreen(
    viewModel: MovieViewModel,
    onSettingsClick: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchQuery by viewModel.watchedSearchQuery.collectAsState()
    val sortOrder by viewModel.watchedSortOrder.collectAsState()
    val filterState by viewModel.watchedFilterState.collectAsState()
    val watchedMovies by viewModel.filteredWatched.collectAsState()
    val totalWatchedCount by viewModel.watchedMovies.collectAsState()

    var showSortFilterSheet by remember { mutableStateOf(false) }
    var movieToDelete by remember { mutableStateOf<Movie?>(null) }
    var movieToEdit by remember { mutableStateOf<Movie?>(null) }
    var movieForCategories by remember { mutableStateOf<Movie?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopBar(
                title = "Watched",
                onSettingsClick = onSettingsClick,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setWatchedSearchQuery(it) },
                placeholder = "Search watched movies...",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section Header: Title, Count, and Sort/Filter Button ──────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Watched",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val countText = if (watchedMovies.size == 1) "1 movie" else "${watchedMovies.size} movies"
                    Text(
                        text = if (searchQuery.isNotBlank() || filterState.isActive) "$countText filtered" else countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = { showSortFilterSheet = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Sort and filter",
                        tint = if (filterState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (watchedMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (searchQuery.isNotBlank()) {
                        EmptyStateMessage(
                            icon = Icons.Outlined.SearchOff,
                            headline = "No movies found",
                            body = "No movie in this list matches\n\"$searchQuery\".",
                        )
                    } else if (filterState.isActive) {
                        EmptyStateMessage(
                            icon = Icons.Outlined.Tune,
                            headline = "No matching movies",
                            body = "No watched movies match the selected filters.",
                            action = {
                                Button(
                                    onClick = { viewModel.resetWatchedSortAndFilter() },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Reset Filters")
                                }
                            },
                        )
                    } else {
                        EmptyStateMessage(
                            icon = Icons.Outlined.CheckCircle,
                            headline = "No watched movies yet",
                            body = "Movies you finish watching\nwill appear here.",
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(watchedMovies, key = { it.id }) { movie ->
                        val watchedDateStr = movie.dateWatched?.let { "Watched on ${DateFormatter.formatDate(it)}" }
                        MovieRow(
                            movie = movie,
                            isChecked = true,
                            onCheckedChange = { viewModel.markUnwatched(movie) },
                            onMovieClick = { onMovieClick(movie) },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            onEditClick = { movieToEdit = movie },
                            onToggleWatched = { viewModel.markUnwatched(movie) },
                            onManageCategoriesClick = { movieForCategories = movie },
                            onDeleteClick = { movieToDelete = movie },
                            subtitleOverride = watchedDateStr,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs & Bottom Sheet ────────────────────────────────────────────────

    if (showSortFilterSheet) {
        SortFilterBottomSheet(
            onDismissRequest = { showSortFilterSheet = false },
            currentSort = sortOrder,
            currentFilters = filterState,
            onApply = { sort, filters ->
                viewModel.setWatchedSortAndFilter(sort, filters)
            },
            onReset = {
                viewModel.resetWatchedSortAndFilter()
            },
        )
    }

    movieToEdit?.let { movie ->
        EditMovieDialog(
            movie = movie,
            viewModel = viewModel,
            onDismiss = { movieToEdit = null },
        )
    }

    movieForCategories?.let { movie ->
        ManageMovieCategoriesDialog(
            movie = movie,
            viewModel = viewModel,
            onDismiss = { movieForCategories = null },
        )
    }

    movieToDelete?.let { movie ->
        DeleteMovieDialog(
            movie = movie,
            onConfirm = {
                viewModel.deleteMovie(movie)
                movieToDelete = null
            },
            onDismiss = { movieToDelete = null },
        )
    }
}
