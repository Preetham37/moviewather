package com.example.moviewather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Dialog enabling the user to assign/unassign a movie across multiple categories.
 */
@Composable
fun ManageMovieCategoriesDialog(
    movie: Movie,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
) {
    val allCategories by viewModel.allCategories.collectAsState()
    val assignedCategoryIds by viewModel.getCategoryIdsForMovie(movie.id).collectAsState(initial = emptyList())

    // Local selected set initialized with the movie's currently assigned category IDs
    var selectedCategoryIds by remember(assignedCategoryIds) {
        mutableStateOf(assignedCategoryIds.toSet())
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Categories",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
            ) {
                if (allCategories.isEmpty()) {
                    Text(
                        text = "You haven't created any categories yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Text("Create Category")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(allCategories, key = { it.id }) { category ->
                            val isChecked = selectedCategoryIds.contains(category.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategoryIds = if (isChecked) {
                                            selectedCategoryIds - category.id
                                        } else {
                                            selectedCategoryIds + category.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedCategoryIds = if (checked) {
                                            selectedCategoryIds + category.id
                                        } else {
                                            selectedCategoryIds - category.id
                                        }
                                    },
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Text("+ New Category")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.setMovieCategories(movie.id, selectedCategoryIds)
                    onDismiss()
                }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
