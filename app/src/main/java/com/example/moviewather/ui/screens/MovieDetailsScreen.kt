package com.example.moviewather.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.metadata.MovieMetadataResult
import com.example.moviewather.data.metadata.OfflineException
import com.example.moviewather.ui.components.DeleteMovieDialog
import com.example.moviewather.ui.components.EditMovieDialog
import com.example.moviewather.ui.components.ManageMovieCategoriesDialog
import com.example.moviewather.ui.components.MetadataFailureDialog
import com.example.moviewather.ui.components.OfflineMetadataDialog
import com.example.moviewather.ui.components.SelectMovieIdentificationDialog
import com.example.moviewather.ui.util.DateFormatter
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

/**
 * Dedicated Movie Details screen.
 *
 * Displays personal collection information (Title, Year, Watch Status, Favorite, Dates, Notes, Categories)
 * and rich cached offline movie metadata (Poster, Director, Genres, Cast, Overview, Runtime) when available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: Long,
    viewModel: MovieViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val movie by viewModel.getMovieById(movieId).collectAsState(initial = null)
    val movieCategories by viewModel.getCategoriesForMovie(movieId).collectAsState(initial = emptyList())
    var hasLoadedInitially by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }

    // Identification & Refresh state
    var isSearchingMetadata by remember { mutableStateOf(false) }
    var isRefreshingMetadata by remember { mutableStateOf(false) }
    var metadataCandidates by remember { mutableStateOf<List<MovieMetadataResult>?>(null) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var metadataError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(movie) {
        if (movie != null) {
            hasLoadedInitially = true
        } else if (hasLoadedInitially) {
            // Movie was deleted, return to collection list
            onNavigateUp()
        }
    }

    val currentMovie = movie
    if (currentMovie == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    fun startIdentificationSearch() {
        isSearchingMetadata = true
        coroutineScope.launch {
            val result = viewModel.searchMetadata(currentMovie.title)
            isSearchingMetadata = false
            result.fold(
                onSuccess = { list ->
                    metadataCandidates = list
                },
                onFailure = { err ->
                    if (err is OfflineException) {
                        showOfflineDialog = true
                    } else {
                        metadataError = err.localizedMessage ?: "Failed to find movie information."
                    }
                }
            )
        }
    }

    fun triggerRefreshMetadata() {
        isRefreshingMetadata = true
        viewModel.refreshMovieMetadata(currentMovie) { result ->
            isRefreshingMetadata = false
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Movie information refreshed.", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    if (err is OfflineException) {
                        showOfflineDialog = true
                    } else {
                        metadataError = err.localizedMessage ?: "Failed to refresh movie information."
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Movie Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Quick favorite toggle in app bar
                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentMovie) }
                    ) {
                        Icon(
                            imageVector = if (currentMovie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (currentMovie.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (currentMovie.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Edit movie action
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit movie",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // ── 1. Poster & Title Header ──────────────────────────────────────
            if (!currentMovie.posterUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentMovie.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentMovie.title,
                        modifier = Modifier
                            .size(width = 160.dp, height = 240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Text(
                text = currentMovie.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Release year, runtime, and genres line
            val headerDetails = buildList {
                currentMovie.releaseYear?.let { add(it.toString()) }
                currentMovie.runtime?.let { add("$it min") }
                if (!currentMovie.genres.isNullOrBlank()) add(currentMovie.genres)
            }.joinToString(" • ")

            if (headerDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headerDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Director and cast info
            if (!currentMovie.director.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Directed by ${currentMovie.director}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!currentMovie.mainCast.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Starring ${currentMovie.mainCast}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Favorite button row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleFavorite(currentMovie) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (currentMovie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (currentMovie.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentMovie.isFavorite) "Favorite" else "Add to Favorites",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (currentMovie.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Plot overview if available
            if (!currentMovie.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Text(
                        text = currentMovie.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Movie Identification / Metadata Status ─────────────────────
            if (currentMovie.externalId == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "Movie information not available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { startIdentificationSearch() },
                            enabled = !isSearchingMetadata,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isSearchingMetadata) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching...")
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search Movie Information")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Identified via ${currentMovie.metadataProvider ?: "TMDb"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TextButton(
                        onClick = { triggerRefreshMetadata() },
                        enabled = !isRefreshingMetadata,
                    ) {
                        if (isRefreshingMetadata) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refreshing...")
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Info")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Movie Status ───────────────────────────────────────────────
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (currentMovie.isWatched) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (currentMovie.isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (currentMovie.isWatched) "Watched" else "Want to Watch",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4. Date Information ───────────────────────────────────────────
            val addedDate = DateFormatter.formatDate(currentMovie.dateAdded)
            if (addedDate.isNotBlank()) {
                Text(
                    text = "Added",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = addedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (currentMovie.isWatched && currentMovie.dateWatched != null) {
                val watchedDate = DateFormatter.formatDate(currentMovie.dateWatched)
                if (watchedDate.isNotBlank()) {
                    Text(
                        text = "Watched",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = watchedDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 5. Watch / Unwatch Primary Action ─────────────────────────────
            if (currentMovie.isWatched) {
                OutlinedButton(
                    onClick = { viewModel.markUnwatched(currentMovie) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Unwatched")
                }
            } else {
                Button(
                    onClick = { viewModel.markWatched(currentMovie) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Watched")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── 6. Categories Section ─────────────────────────────────────────
            CategoriesSection(
                categories = movieCategories,
                onEditCategories = { showCategoriesDialog = true },
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── 7. Notes Section ──────────────────────────────────────────────
            NotesSection(
                movie = currentMovie,
                onSaveNotes = { newNotes -> viewModel.updateNotes(currentMovie, newNotes) },
            )

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── 8. Secondary Management Actions ───────────────────────────────
            OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Movie Information")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Movie",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showCategoriesDialog) {
        ManageMovieCategoriesDialog(
            movie = currentMovie,
            viewModel = viewModel,
            onDismiss = { showCategoriesDialog = false },
        )
    }

    if (showEditDialog) {
        EditMovieDialog(
            movie = currentMovie,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteMovieDialog(
            movie = currentMovie,
            onConfirm = {
                viewModel.deleteMovie(currentMovie)
                showDeleteDialog = false
                onNavigateUp()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    // Identification candidate selection dialog
    metadataCandidates?.let { candidates ->
        SelectMovieIdentificationDialog(
            query = currentMovie.title,
            candidates = candidates,
            onSelectCandidate = { candidate ->
                coroutineScope.launch {
                    val updated = currentMovie.copy(
                        title = candidate.title,
                        releaseYear = candidate.releaseYear,
                        posterUrl = candidate.posterUrl,
                        overview = candidate.overview,
                        genres = if (candidate.genres.isNotEmpty()) candidate.genresDisplay else null,
                        director = candidate.director,
                        mainCast = if (candidate.mainCast.isNotEmpty()) candidate.mainCastDisplay else null,
                        runtime = candidate.runtime,
                        externalId = candidate.externalId,
                        metadataProvider = candidate.provider,
                        metadataLastUpdated = System.currentTimeMillis(),
                    )
                    viewModel.updateMovie(updated)
                    metadataCandidates = null
                }
            },
            onSaveTitleOnly = {
                metadataCandidates = null
            },
            onDismiss = {
                metadataCandidates = null
            },
        )
    }

    // Offline dialog
    if (showOfflineDialog) {
        OfflineMetadataDialog(
            onSaveTitleOnly = { showOfflineDialog = false },
            onDismiss = { showOfflineDialog = false },
        )
    }

    // Metadata error dialog
    metadataError?.let { err ->
        MetadataFailureDialog(
            errorMessage = err,
            onSaveTitleOnly = { metadataError = null },
            onTryAgain = {
                metadataError = null
                if (currentMovie.externalId != null) {
                    triggerRefreshMetadata()
                } else {
                    startIdentificationSearch()
                }
            },
            onDismiss = { metadataError = null },
        )
    }
}

/**
 * Notes management section allowing adding, editing, clearing, and saving personal movie notes.
 */
@Composable
private fun NotesSection(
    movie: Movie,
    onSaveNotes: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember(movie.id) { mutableStateOf(false) }
    var notesText by remember(movie.notes) { mutableStateOf(movie.notes) }
    val hasUnsavedChanges = notesText != movie.notes

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!isEditing && movie.notes.isNotBlank()) {
                TextButton(onClick = { isEditing = true }) {
                    Text("Edit Notes")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditing && movie.notes.isBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isEditing = true }) {
                        Text("+ Add Notes")
                    }
                }
            }
        } else if (!isEditing) {
            // Display formatted notes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Text(
                    text = movie.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            // Editing mode
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add personal thoughts, recommendations, or reminders...") },
                minLines = 3,
                maxLines = 6,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notesText.isNotEmpty()) {
                    TextButton(
                        onClick = { notesText = "" }
                    ) {
                        Text("Clear")
                    }
                }

                TextButton(
                    onClick = {
                        notesText = movie.notes
                        isEditing = false
                    }
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSaveNotes(notesText)
                        isEditing = false
                    },
                    enabled = hasUnsavedChanges,
                ) {
                    Text("Save Notes")
                }
            }
        }
    }
}

/**
 * Displays assigned categories as chips, with an action to edit or add categories.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesSection(
    categories: List<Category>,
    onEditCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (categories.isNotEmpty()) {
                TextButton(onClick = onEditCategories) {
                    Text("Edit Categories")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (categories.isEmpty()) {
            OutlinedButton(
                onClick = onEditCategories,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Category")
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                categories.forEach { category ->
                    SuggestionChip(
                        onClick = onEditCategories,
                        label = { Text(category.name) },
                    )
                }
            }
        }
    }
}
