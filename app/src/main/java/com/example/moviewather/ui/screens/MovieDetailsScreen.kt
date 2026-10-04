package com.example.moviewather.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.moviewather.ui.theme.DarkBackground
import com.example.moviewather.ui.theme.DarkBorder
import com.example.moviewather.ui.theme.DarkCard
import com.example.moviewather.ui.theme.DarkSurfaceVariant
import com.example.moviewather.ui.theme.PrimaryBlue
import com.example.moviewather.ui.util.DateFormatter
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

/**
 * Dedicated Movie Details screen matching Screen 5 in the reference mockup:
 * - Backdrop image banner with bottom fade gradient to dark background
 * - Overlay action icons (Back, Favorite, Edit)
 * - Movie title, release year, and genre pills
 * - Runtime, director, and metadata identification info row
 * - Overview / Plot summary
 * - Cast section with avatar row
 * - Personal Information Card (Watch status, dates, categories, notes)
 * - Prominent bottom action buttons: "Mark as Watched" & "Add / Edit Note"
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
            CircularProgressIndicator(color = PrimaryBlue)
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
        containerColor = DarkBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 1. Backdrop / Poster Banner with Gradient Fade ────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                if (!currentMovie.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentMovie.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentMovie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }

                // Vertical gradient scrim: top subtle scrim for action buttons, bottom fade to DarkBackground
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.8f),
                                    DarkBackground,
                                ),
                                startY = 0f,
                                endY = 800f,
                            )
                        ),
                )

                // Top Navigation and Actions Bar overlaid on backdrop
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Favorite toggle
                        IconButton(
                            onClick = { viewModel.toggleFavorite(currentMovie) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                        ) {
                            Icon(
                                imageVector = if (currentMovie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (currentMovie.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (currentMovie.isFavorite) Color(0xFFFFB800) else Color.White,
                            )
                        }

                        // Edit movie action
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit movie",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            // ── 2. Movie Header: Title, Year, Genre Pills & Metadata Info ─────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                // Title and Release Year
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentMovie.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (currentMovie.releaseYear != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${currentMovie.releaseYear})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = PrimaryBlue,
                            ),
                        )
                    }
                }

                // Genre Pills Row
                val genreList = currentMovie.genres
                    ?.split(Regex("[,•·]"))
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                if (genreList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        genreList.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                // Metadata Details Row: Runtime, Director, Provider
                val metaItems = buildList {
                    currentMovie.runtime?.let { add("⏱ $it min") }
                    currentMovie.director?.takeIf { it.isNotBlank() }?.let { add("🎬 Dir. $it") }
                }

                if (metaItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        metaItems.forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                        }
                    }
                }

                // ── Metadata Provider Status & Refresh ────────────────────────
                Spacer(modifier = Modifier.height(8.dp))
                if (currentMovie.externalId == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Title-only (unidentified)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            ),
                        )
                        TextButton(
                            onClick = { startIdentificationSearch() },
                            enabled = !isSearchingMetadata,
                        ) {
                            if (isSearchingMetadata) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Searching...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Identify Movie", fontSize = 12.sp)
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
                            text = "TMDB ID: ${currentMovie.externalId}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                            ),
                        )
                        TextButton(
                            onClick = { triggerRefreshMetadata() },
                            enabled = !isRefreshingMetadata,
                        ) {
                            if (isRefreshingMetadata) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refreshing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh Info", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // ── 3. Overview Section ───────────────────────────────────────
                if (!currentMovie.overview.isNullOrBlank()) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentMovie.overview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 22.sp,
                        ),
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 4. Cast Section ───────────────────────────────────────────
                val castList = currentMovie.mainCast
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                if (castList.isNotEmpty()) {
                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        castList.forEach { actor ->
                            CastAvatarItem(name = actor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 5. Personal Information Card ──────────────────────────────
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        // Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentMovie.isWatched) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = if (currentMovie.isWatched) PrimaryBlue else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentMovie.isWatched) "Watched" else "Movies to Watch",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (currentMovie.isWatched) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }

                        // Date Added
                        val addedDate = DateFormatter.formatDate(currentMovie.dateAdded)
                        if (addedDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Date Added",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                                Text(
                                    text = addedDate,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }

                        // Date Watched
                        if (currentMovie.isWatched && currentMovie.dateWatched != null) {
                            val watchedDate = DateFormatter.formatDate(currentMovie.dateWatched)
                            if (watchedDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Date Watched",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                    Text(
                                        text = watchedDate,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }
                            }
                        }

                        // Categories in personal card
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                            TextButton(
                                onClick = { showCategoriesDialog = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (movieCategories.isEmpty()) "+ Add" else "Edit",
                                    color = PrimaryBlue,
                                    fontSize = 13.sp,
                                )
                            }
                        }

                        if (movieCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                movieCategories.forEach { category ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 6. Notes Section ──────────────────────────────────────────
                Spacer(modifier = Modifier.height(18.dp))
                NotesSection(
                    movie = currentMovie,
                    onSaveNotes = { newNotes -> viewModel.updateNotes(currentMovie, newNotes) },
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── 7. Primary Action Buttons (Screen 5 layout) ────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (currentMovie.isWatched) {
                        OutlinedButton(
                            onClick = { viewModel.markUnwatched(currentMovie) },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unwatch", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.markWatched(currentMovie) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Watched", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete button
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Movie",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
 * Cast member circular avatar and name component matching Screen 5.
 */
@Composable
private fun CastAvatarItem(
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )

            if (!isEditing && movie.notes.isNotBlank()) {
                TextButton(onClick = { isEditing = true }) {
                    Text("Edit Notes", color = PrimaryBlue, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditing && movie.notes.isBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .clickable { isEditing = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+ Add personal thoughts or reminders",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        } else if (!isEditing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .clickable { isEditing = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
            ) {
                Text(
                    text = movie.notes,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            // Editing mode
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Add personal thoughts, recommendations, or reminders...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notesText.isNotEmpty()) {
                    TextButton(onClick = { notesText = "" }) {
                        Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                TextButton(
                    onClick = {
                        notesText = movie.notes
                        isEditing = false
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSaveNotes(notesText)
                        isEditing = false
                    },
                    enabled = hasUnsavedChanges,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Save Notes")
                }
            }
        }
    }
}
