package com.example.moviewather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moviewather.data.metadata.MovieMetadataResult
import com.example.moviewather.data.metadata.OfflineException
import com.example.moviewather.data.util.DuplicateCheckResult
import com.example.moviewather.ui.components.DuplicateWarningDialog
import com.example.moviewather.ui.components.MetadataFailureDialog
import com.example.moviewather.ui.components.OfflineMetadataDialog
import com.example.moviewather.ui.components.SelectMovieIdentificationDialog
import com.example.moviewather.ui.theme.DarkBorder
import com.example.moviewather.ui.theme.DarkCard
import com.example.moviewather.ui.theme.DarkSurfaceVariant
import com.example.moviewather.ui.theme.PrimaryBlue
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
 * Add Movie screen matching Screen 3 of the reference mockup:
 * - Segmented pill tabs: [ Single ] [ Bulk Add ] [ From List ]
 * - Single movie mode: Movie Title input, "Search Movie Information" blue button, "── or ──",
 *   "Save Title Only" card button with "Add without searching" subtitle, metadata preview card,
 *   and "How it works?" guide.
 * - Bulk Add mode: multiline input with duplicate review workflow.
 * - From List mode: fast multi-item / comma-separated list adder.
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

    // From List state
    var fromListText by remember { mutableStateOf("") }

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
                        text = if (isReviewingBulk) "Review Movies" else "Add Movie",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        ),
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
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
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
                // ── Segmented Pill Tab Row: [ Single ] [ Bulk Add ] [ From List ] ──
                SegmentedPillTabBar(
                    tabs = listOf("Single", "Bulk Add", "From List"),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )

                Spacer(modifier = Modifier.height(20.dp))

                when (selectedTab) {
                    0 -> {
                        // ── Tab 0: Single Movie Form ──────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "Movie Title",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = singleTitle,
                                onValueChange = { singleTitle = it },
                                placeholder = {
                                    Text(
                                        text = "Enter movie title...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Movie,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                trailingIcon = {
                                    if (singleTitle.isNotEmpty()) {
                                        IconButton(onClick = { singleTitle = "" }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkCard,
                                    unfocusedContainerColor = DarkCard,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // "Search Movie Information" Blue Button
                            Button(
                                onClick = { startMetadataSearch(singleTitle) },
                                enabled = singleTitle.isNotBlank() && !isSearchingMetadata,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White,
                                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                if (isSearchingMetadata) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Searching TMDB...",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Search Movie Information",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            // Display Selected Metadata preview card if movie was identified
                            selectedMetadata?.let { meta ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = DarkCard,
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
                                                    .size(width = 46.dp, height = 68.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = meta.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                if (meta.releaseYear != null) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "(${meta.releaseYear})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = PrimaryBlue,
                                                    )
                                                }
                                            }
                                            if (!meta.director.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Directed by ${meta.director}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            if (meta.genres.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = meta.genresDisplay,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // "── or ──" Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = DarkBorder,
                                )
                                Text(
                                    text = "or",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = DarkBorder,
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // "Save Title Only" Card Button (Screen 3 style)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable(enabled = singleTitle.isNotBlank()) {
                                        commitSaveMovie(singleTitle, singleNotes, selectedMetadata)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkCard,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Save Title Only",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (singleTitle.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            ),
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Add without searching",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            ),
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null,
                                        tint = if (singleTitle.isNotBlank()) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            // Optional Notes input
                            Spacer(modifier = Modifier.height(18.dp))
                            OutlinedTextField(
                                value = singleNotes,
                                onValueChange = { singleNotes = it },
                                label = { Text("Notes (optional)") },
                                placeholder = {
                                    Text(
                                        "e.g. Recommended by a friend",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                },
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkCard,
                                    unfocusedContainerColor = DarkCard,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // If metadata was selected, show a primary Add to Watchlist button
                            if (selectedMetadata != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        commitSaveMovie(singleTitle, singleNotes, selectedMetadata)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text("Add to Watchlist", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // "How it works?" 3-Step Guide Card (Screen 3 style)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkSurfaceVariant.copy(alpha = 0.4f),
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "How it works?",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    StepRow(number = "1", text = "Enter the title of any movie you want to watch.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StepRow(number = "2", text = "Search TMDB to fetch poster and metadata, or save title only for offline use.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StepRow(number = "3", text = "Organize with categories and track your watching progress.")
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    1 -> {
                        // ── Tab 1: Bulk Add Input Form ────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
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
                                        "Interstellar\nInception\nDune\nOppenheimer\nThe Dark Knight",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                },
                                minLines = 8,
                                maxLines = 14,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkCard,
                                    unfocusedContainerColor = DarkCard,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(20.dp))

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
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                Text("Review Movies", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    2 -> {
                        // ── Tab 2: From List Form ────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "Quickly paste comma-separated or numbered movie titles:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fromListText,
                                onValueChange = { fromListText = it },
                                placeholder = {
                                    Text(
                                        "Avatar, Titanic, Gladiator, The Matrix, Pulp Fiction",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                },
                                minLines = 6,
                                maxLines = 10,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkCard,
                                    unfocusedContainerColor = DarkCard,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    // Parse comma-separated or newline-separated items
                                    val items = fromListText
                                        .split(Regex("[,\\n]"))
                                        .map { it.trim().trimStart { c -> c.isDigit() || c == '.' || c == '-' || c == ')' } }
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    bulkReviewList.clear()
                                    items.forEach { title ->
                                        val check = viewModel.checkDuplicate(title)
                                        bulkReviewList.add(BulkReviewItem(title = title, checkResult = check))
                                    }

                                    if (bulkReviewList.isNotEmpty()) {
                                        isReviewingBulk = true
                                    }
                                },
                                enabled = fromListText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                Text("Process List", fontWeight = FontWeight.SemiBold)
                            }
                        }
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text("Add to Watchlist", fontWeight = FontWeight.SemiBold)
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
 * Segmented Pill Tab Bar matching the reference mockup:
 * Pill container with rounded capsule shape and blue indicator for active tab.
 */
@Composable
private fun SegmentedPillTabBar(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, DarkBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) PrimaryBlue else Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryBlue.copy(alpha = 0.15f),
            modifier = Modifier.size(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            ),
            modifier = Modifier.weight(1f),
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
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard,
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
                        tint = PrimaryBlue,
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
