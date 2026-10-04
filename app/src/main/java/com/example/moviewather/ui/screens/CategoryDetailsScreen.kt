package com.example.moviewather.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.ui.components.DeleteCategoryDialog
import com.example.moviewather.ui.components.EditCategoryDialog
import com.example.moviewather.ui.components.EmptyStateMessage
import com.example.moviewather.ui.components.SearchBar
import com.example.moviewather.ui.components.SortDropdownButton
import com.example.moviewather.ui.model.WatchlistSortOrder
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Dedicated Category Details screen displaying all movies assigned to a category.
 *
 * Supports local search within the category, presentation sorting,
 * watched-status toggling, removal from category, and category renaming/deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: Long,
    viewModel: MovieViewModel,
    onMovieClick: (Movie) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category by viewModel.getCategoryById(categoryId).collectAsState(initial = null)
    var hasLoadedInitially by remember { mutableStateOf(false) }

    val searchQueryState = remember { MutableStateFlow("") }
    val sortOrderState = remember { MutableStateFlow(WatchlistSortOrder.RECENTLY_ADDED) }

    val searchQuery by searchQueryState.collectAsState()
    val sortOrder by sortOrderState.collectAsState()

    val filteredMovies by remember(categoryId) {
        viewModel.getFilteredMoviesForCategory(categoryId, searchQueryState, sortOrderState)
    }.collectAsState(initial = emptyList())

    val allCategoryMovies by remember(categoryId) {
        viewModel.getMoviesForCategory(categoryId)
    }.collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(category) {
        if (category != null) {
            hasLoadedInitially = true
        } else if (hasLoadedInitially) {
            // Category was deleted, navigate back
            onNavigateUp()
        }
    }

    val currentCategory = category
    if (currentCategory == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(currentCategory.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Rename category",
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
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
                onQueryChange = { searchQueryState.value = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Header: Count and Sort Dropdown ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = currentCategory.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (allCategoryMovies.isNotEmpty()) {
                        val countText = if (filteredMovies.size == 1) "1 movie" else "${filteredMovies.size} movies"
                        Text(
                            text = if (searchQuery.isNotBlank()) "$countText found" else countText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (allCategoryMovies.isNotEmpty()) {
                    SortDropdownButton(
                        options = WatchlistSortOrder.entries,
                        selected = sortOrder,
                        onSelect = { sortOrderState.value = it },
                        labelProvider = { it.label },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredMovies.isEmpty()) {
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
                            body = "No movie in this category matches\n\"$searchQuery\".",
                        )
                    } else {
                        EmptyStateMessage(
                            icon = Icons.Outlined.Movie,
                            headline = "No movies here",
                            body = "Add movies to this category\nfrom their Movie Details page.",
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredMovies, key = { it.id }) { movie ->
                        CategoryMovieCard(
                            movie = movie,
                            onMovieClick = { onMovieClick(movie) },
                            onToggleWatched = {
                                if (movie.isWatched) {
                                    viewModel.markUnwatched(movie)
                                } else {
                                    viewModel.markWatched(movie)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            onRemoveFromCategory = {
                                viewModel.removeMovieFromCategory(movie.id, currentCategory.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditCategoryDialog(
            category = currentCategory,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteCategoryDialog(
            category = currentCategory,
            onConfirm = {
                viewModel.deleteCategory(currentCategory)
                showDeleteDialog = false
                onNavigateUp()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun CategoryMovieCard(
    movie: Movie,
    onMovieClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onMovieClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = movie.isWatched,
                onCheckedChange = { onToggleWatched() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (movie.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = movie.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (movie.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (movie.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            IconButton(onClick = onRemoveFromCategory) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove from category",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
