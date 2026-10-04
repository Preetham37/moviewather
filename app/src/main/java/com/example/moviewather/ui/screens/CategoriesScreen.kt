package com.example.moviewather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.model.CategoryWithCount
import com.example.moviewather.ui.components.CreateCategoryDialog
import com.example.moviewather.ui.components.EmptyStateMessage
import com.example.moviewather.ui.components.MainTopBar
import com.example.moviewather.ui.theme.CategoryBlue
import com.example.moviewather.ui.theme.CategoryPink
import com.example.moviewather.ui.theme.CategoryPurple
import com.example.moviewather.ui.theme.CategoryRed
import com.example.moviewather.ui.theme.CategoryTeal
import com.example.moviewather.ui.theme.CategoryYellow
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Categories screen matching Screen 6 of the reference mockup.
 *
 * Fast to scan, clean category cards with distinct colorful icons,
 * item counts, and top-bar add action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MovieViewModel,
    onCategoryClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoriesWithCount by viewModel.categoriesWithCount.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopBar(
                title = "Categories",
                onSettingsClick = onSettingsClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Create Category",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
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

            if (categoriesWithCount.isEmpty() && allMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateMessage(
                        icon = Icons.Outlined.Category,
                        headline = "No categories yet",
                        body = "Create categories to organize\nyour movie collection.",
                        action = {
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Category")
                            }
                        },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // "All Movies" top item matching Screen 6
                    item(key = "all_movies_item") {
                        CategoryCardItem(
                            title = "All Movies",
                            count = allMovies.size,
                            icon = Icons.Outlined.Movie,
                            iconTint = CategoryBlue,
                            onClick = {
                                if (categoriesWithCount.isNotEmpty()) {
                                    onCategoryClick(categoriesWithCount.first().category.id)
                                }
                            },
                        )
                    }

                    items(categoriesWithCount, key = { it.category.id }) { item ->
                        val (icon, color) = getCategoryVisuals(item.category.name)
                        CategoryCardItem(
                            title = item.category.name,
                            count = item.movieCount,
                            icon = icon,
                            iconTint = color,
                            onClick = { onCategoryClick(item.category.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
        )
    }
}

@Composable
private fun CategoryCardItem(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon container with colored icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val countLabel = if (count == 1) "1 movie" else "$count movies"
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun getCategoryVisuals(name: String): Pair<ImageVector, Color> {
    val lower = name.lowercase()
    return when {
        lower.contains("action") -> Icons.Outlined.LocalFireDepartment to CategoryPink
        lower.contains("sci-fi") || lower.contains("science") -> Icons.Outlined.Public to CategoryTeal
        lower.contains("drama") -> Icons.Outlined.TheaterComedy to CategoryPurple
        lower.contains("comedy") -> Icons.Outlined.SentimentSatisfied to CategoryYellow
        lower.contains("thriller") || lower.contains("horror") -> Icons.Outlined.TrackChanges to CategoryRed
        else -> Icons.AutoMirrored.Outlined.Label to CategoryBlue
    }
}
