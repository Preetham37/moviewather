package com.example.moviewather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.ui.components.DeleteMovieDialog
import com.example.moviewather.ui.components.EmptyStateMessage
import com.example.moviewather.ui.components.MainTopBar
import com.example.moviewather.ui.components.SearchBar
import com.example.moviewather.ui.components.SortDropdownButton
import com.example.moviewather.ui.model.WatchlistSortOrder
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Watchlist screen – the user's primary unwatched movie collection view.
 *
 * Supports real-time local search, flexible presentation sorting,
 * checkbox marking as watched, quick favorite toggling, and deletion with confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: MovieViewModel,
    onAddMovieClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchQuery by viewModel.watchlistSearchQuery.collectAsState()
    val sortOrder by viewModel.watchlistSortOrder.collectAsState()
    val movies by viewModel.filteredWatchlist.collectAsState()
    val totalUnwatchedCount by viewModel.unwatchedMovies.collectAsState()

    var movieToDelete by remember { mutableStateOf<Movie?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopBar(
                title = "Movie Watcher",
                onSettingsClick = onSettingsClick,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMovieClick,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                    )
                },
                text = { Text("Add Movie") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setWatchlistSearchQuery(it) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Header: Title, Count, and Sort Dropdown ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Movies to Watch",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (totalUnwatchedCount.isNotEmpty()) {
                        val countText = if (movies.size == 1) "1 movie" else "${movies.size} movies"
                        Text(
                            text = if (searchQuery.isNotBlank()) "$countText found" else countText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (totalUnwatchedCount.isNotEmpty()) {
                    SortDropdownButton(
                        options = WatchlistSortOrder.entries,
                        selected = sortOrder,
                        onSelect = { viewModel.setWatchlistSortOrder(it) },
                        labelProvider = { it.label },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (movies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (searchQuery.isNotBlank()) {
                        // Empty search results state
                        EmptyStateMessage(
                            icon = Icons.Outlined.SearchOff,
                            headline = "No movies found",
                            body = "No movie in this list matches\n\"$searchQuery\".",
                        )
                    } else {
                        // Watchlist empty state
                        EmptyStateMessage(
                            icon = Icons.Outlined.Movie,
                            headline = "No movies to watch",
                            body = "Movies you want to watch\nwill appear here.",
                            action = {
                                Button(onClick = onAddMovieClick) {
                                    Text("Add Movie")
                                }
                            },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(movies, key = { it.id }) { movie ->
                        WatchlistMovieCard(
                            movie = movie,
                            onMovieClick = { onMovieClick(movie) },
                            onMarkWatched = { viewModel.markWatched(movie) },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            onDelete = { movieToDelete = movie },
                            modifier = Modifier.animateItem(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
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

@Composable
private fun WatchlistMovieCard(
    movie: Movie,
    onMovieClick: () -> Unit,
    onMarkWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onMovieClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onMarkWatched() },
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Consistent poster thumbnail / placeholder slot for visual alignment
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .size(width = 40.dp, height = 58.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 58.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (movie.releaseYear != null || movie.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (movie.releaseYear != null) {
                            Text(
                                text = movie.releaseYear.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (movie.releaseYear != null && movie.notes.isNotBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        if (movie.notes.isNotBlank()) {
                            Text(
                                text = movie.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (movie.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (movie.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${movie.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
