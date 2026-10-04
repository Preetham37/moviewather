package com.example.moviewather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moviewather.data.metadata.MovieMetadataResult
import com.example.moviewather.data.metadata.OfflineException
import com.example.moviewather.data.util.DuplicateCheckResult
import com.example.moviewather.ui.components.DuplicateWarningDialog
import com.example.moviewather.ui.components.MetadataFailureDialog
import com.example.moviewather.ui.components.OfflineMetadataDialog
import com.example.moviewather.ui.components.SelectMovieIdentificationDialog
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

/**
 * Item in the bulk review list with its duplicate check status.
 */
data class BulkReviewItem(
    val title: String,
    val checkResult: DuplicateCheckResult,
)

/**
 * Add Movie screen – allows adding a single movie with movie identification,
 * duplicate/similarity warnings, or bulk importing a list of movies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieScreen(
    viewModel: MovieViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Single movie form state
    var singleTitle by remember { mutableStateOf("") }
    var singleNotes by remember { mutableStateOf("") }
    var selectedMetadata by remember { mutableStateOf<MovieMetadataResult?>(null) }
    var isSearchingMetadata by remember { mutableStateOf(false) }
    var metadataCandidates by remember { mutableStateOf<List<MovieMetadataResult>?>(null) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var metadataError by remember { mutableStateOf<String?>(null) }
    var pendingDuplicateResult by remember { mutableStateOf<DuplicateCheckResult?>(null) }

    // Bulk add state
    var bulkText by remember { mutableStateOf("") }
    var isReviewingBulk by remember { mutableStateOf(false) }
    val bulkReviewList = remember { mutableStateListOf<BulkReviewItem>() }

    fun commitSaveMovie(title: String, notes: String, metadata: MovieMetadataResult?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val check = viewModel.checkDuplicate(
            title = trimmed,
            candidateYear = metadata?.releaseYear,
            candidateExternalId = metadata?.externalId,
        )
        if (check is DuplicateCheckResult.None) {
            viewModel.addMovieWithMetadata(trimmed, notes, metadata)
            onNavigateUp()
        } else {
            pendingDuplicateResult = check
        }
    }

    fun startMetadataSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        isSearchingMetadata = true
        coroutineScope.launch {
            val result = viewModel.searchMetadata(trimmed)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isReviewingBulk) "Review Movies" else "Add Movies",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isReviewingBulk) {
                            isReviewingBulk = false
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (!isReviewingBulk) {
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Single Movie") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Add Multiple") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // ── Single Movie Form ─────────────────────────────────────
                    OutlinedTextField(
                        value = singleTitle,
                        onValueChange = { singleTitle = it },
                        label = { Text("Movie Title") },
                        placeholder = { Text("e.g. Dune") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Explicit Movie Identification Action
                    OutlinedButton(
                        onClick = { startMetadataSearch(singleTitle) },
                        enabled = singleTitle.isNotBlank() && !isSearchingMetadata,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isSearchingMetadata) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching Movie Information...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search Movie Information")
                        }
                    }

                    // Display Selected Metadata preview card if movie was identified
                    selectedMetadata?.let { meta ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!meta.posterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = meta.posterUrl,
                                        contentDescription = meta.title,
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 66.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = meta.title,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        if (meta.releaseYear != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${meta.releaseYear})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    if (!meta.director.isNullOrBlank()) {
                                        Text(
                                            text = "Directed by ${meta.director}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (meta.genres.isNotEmpty()) {
                                        Text(
                                            text = meta.genresDisplay,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { selectedMetadata = null }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove metadata",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = singleNotes,
                        onValueChange = { singleNotes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Recommended by a friend") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            commitSaveMovie(singleTitle, singleNotes, selectedMetadata)
                        },
                        enabled = singleTitle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add to Watchlist")
                    }
                } else {
                    // ── Bulk Add Input Form ───────────────────────────────────
                    Text(
                        text = "Paste or type a list of movie titles, one per line:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        placeholder = {
                            Text(
                                "Interstellar\nInception\nDune\nOppenheimer",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val lines = bulkText
                                .lines()
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            bulkReviewList.clear()
                            lines.forEach { title ->
                                val check = viewModel.checkDuplicate(title)
                                bulkReviewList.add(BulkReviewItem(title = title, checkResult = check))
                            }

                            if (bulkReviewList.isNotEmpty()) {
                                isReviewingBulk = true
                            }
                        },
                        enabled = bulkText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Review Movies")
                    }
                }
            } else {
                // ── Bulk Review Screen ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countText = if (bulkReviewList.size == 1) "1 movie" else "${bulkReviewList.size} movies"
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    itemsIndexed(bulkReviewList) { index, item ->
                        BulkReviewItemCard(
                            item = item,
                            onRemove = { bulkReviewList.removeAt(index) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { isReviewingBulk = false },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Edit List")
                    }

                    Button(
                        onClick = {
                            val moviesToAdd = bulkReviewList.map { it.title to "" }
                            viewModel.addMultipleMovies(moviesToAdd)
                            onNavigateUp()
                        },
                        enabled = bulkReviewList.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Add to Watchlist")
                    }
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    // 1. Candidate Selection Dialog ("Which movie did you mean?")
    metadataCandidates?.let { candidates ->
        SelectMovieIdentificationDialog(
            query = singleTitle.trim(),
            candidates = candidates,
            onSelectCandidate = { candidate ->
                selectedMetadata = candidate
                metadataCandidates = null
            },
            onSaveTitleOnly = {
                metadataCandidates = null
                commitSaveMovie(singleTitle, singleNotes, null)
            },
            onDismiss = {
                metadataCandidates = null
            },
        )
    }

    // 2. Offline Information Dialog
    if (showOfflineDialog) {
        OfflineMetadataDialog(
            onSaveTitleOnly = {
                showOfflineDialog = false
                commitSaveMovie(singleTitle, singleNotes, null)
            },
            onDismiss = {
                showOfflineDialog = false
            },
        )
    }

    // 3. Metadata Failure / Error Dialog
    metadataError?.let { err ->
        MetadataFailureDialog(
            errorMessage = err,
            onSaveTitleOnly = {
                metadataError = null
                commitSaveMovie(singleTitle, singleNotes, null)
            },
            onTryAgain = {
                metadataError = null
                startMetadataSearch(singleTitle)
            },
            onDismiss = {
                metadataError = null
            },
        )
    }

    // 4. Duplicate / Similarity Warning Dialog
    pendingDuplicateResult?.let { result ->
        DuplicateWarningDialog(
            checkResult = result,
            candidateTitle = singleTitle.trim(),
            onConfirm = {
                val trimmed = singleTitle.trim()
                viewModel.addMovieWithMetadata(trimmed, singleNotes, selectedMetadata)
                pendingDuplicateResult = null
                onNavigateUp()
            },
            onDismiss = {
                pendingDuplicateResult = null
            },
        )
    }
}

/**
 * Card representing a movie candidate in the bulk review screen.
 */
@Composable
private fun BulkReviewItemCard(
    item: BulkReviewItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val check = item.checkResult) {
                is DuplicateCheckResult.ExternalIdMatch,
                is DuplicateCheckResult.ExactTitleAndYearMatch,
                is DuplicateCheckResult.ExactMatch -> {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Duplicate warning",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                is DuplicateCheckResult.SimilarMatch -> {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Similar movie warning",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                DuplicateCheckResult.None -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "New movie",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(horizontal = 6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                when (val check = item.checkResult) {
                    is DuplicateCheckResult.ExternalIdMatch -> {
                        val status = if (check.isWatched) "Watched" else "Watchlist"
                        Text(
                            text = "Exact movie in $status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is DuplicateCheckResult.ExactTitleAndYearMatch -> {
                        val status = if (check.isWatched) "Watched" else "Watchlist"
                        Text(
                            text = "Same title & year in $status (${check.releaseYear})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is DuplicateCheckResult.ExactMatch -> {
                        val status = if (check.isWatched) "Watched" else "Watchlist"
                        Text(
                            text = "Already in collection ($status)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is DuplicateCheckResult.SimilarMatch -> {
                        Text(
                            text = "Similar: \"${check.existingMovie.title}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    DuplicateCheckResult.None -> {
                        Text(
                            text = "New movie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove ${item.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
