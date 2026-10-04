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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.ui.components.DeleteCategoryDialog
import com.example.moviewather.ui.components.DeleteMovieDialog
import com.example.moviewather.ui.components.EditCategoryDialog
import com.example.moviewather.ui.components.EditMovieDialog
import com.example.moviewather.ui.components.EmptyStateMessage
import com.example.moviewather.ui.components.ManageMovieCategoriesDialog
import com.example.moviewather.ui.components.MovieRow
import com.example.moviewather.ui.components.SearchBar
import com.example.moviewather.ui.components.SortFilterBottomSheet
import com.example.moviewather.ui.model.MovieFilterState
import com.example.moviewather.ui.model.MovieSortOption
import com.example.moviewather.ui.util.DateFormatter
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Category Details screen matching Screen 7 of the reference mockup.
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
    val sortOrderState = remember { MutableStateFlow(MovieSortOption.RECENTLY_ADDED) }

    val searchQuery by searchQueryState.collectAsState()
    val sortOrder by sortOrderState.collectAsState()

    val filteredMovies by remember(categoryId) {
        viewModel.getFilteredMoviesForCategory(categoryId, searchQueryState, sortOrderState)
    }.collectAsState(initial = emptyList())

    val allCategoryMovies by remember(categoryId) {
        viewModel.getMoviesForCategory(categoryId)
    }.collectAsState(initial = emptyList())

    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    var movieToDelete by remember { mutableStateOf<Movie?>(null) }
    var movieToEdit by remember { mutableStateOf<Movie?>(null) }
    var movieForCategories by remember { mutableStateOf<Movie?>(null) }

    LaunchedEffect(category) {
        if (category != null) {
            hasLoadedInitially = true
        } else if (hasLoadedInitially) {
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
                title = {
                    Column {
                        Text(
                            text = currentCategory.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        val countLabel = if (allCategoryMovies.size == 1) "1 movie" else "${allCategoryMovies.size} movies"
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditCategoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Rename category",
                        )
                    }
                    IconButton(onClick = { showDeleteCategoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
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
            Spacer(modifier = Modifier.height(8.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQueryState.value = it },
                placeholder = "Search in ${currentCategory.name}...",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Header: Count and Sort/Filter Button ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${filteredMovies.size} movies",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(
                    onClick = { showSortFilterSheet = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
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
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredMovies, key = { it.id }) { movie ->
                        val watchedDateStr = if (movie.isWatched && movie.dateWatched != null) {
                            "Watched on ${DateFormatter.formatDate(movie.dateWatched)}"
                        } else null

                        MovieRow(
                            movie = movie,
                            isChecked = movie.isWatched,
                            onCheckedChange = {
                                if (movie.isWatched) viewModel.markUnwatched(movie) else viewModel.markWatched(movie)
                            },
                            onMovieClick = { onMovieClick(movie) },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            onEditClick = { movieToEdit = movie },
                            onToggleWatched = {
                                if (movie.isWatched) viewModel.markUnwatched(movie) else viewModel.markWatched(movie)
                            },
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

    if (showSortFilterSheet) {
        SortFilterBottomSheet(
            onDismissRequest = { showSortFilterSheet = false },
            currentSort = sortOrder,
            currentFilters = MovieFilterState(),
            onApply = { sort, _ ->
                sortOrderState.value = sort
            },
            onReset = {
                sortOrderState.value = MovieSortOption.RECENTLY_ADDED
            },
        )
    }

    if (showEditCategoryDialog) {
        EditCategoryDialog(
            category = currentCategory,
            viewModel = viewModel,
            onDismiss = { showEditCategoryDialog = false },
        )
    }

    if (showDeleteCategoryDialog) {
        DeleteCategoryDialog(
            category = currentCategory,
            onConfirm = {
                viewModel.deleteCategory(currentCategory)
                showDeleteCategoryDialog = false
                onNavigateUp()
            },
            onDismiss = { showDeleteCategoryDialog = false },
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
