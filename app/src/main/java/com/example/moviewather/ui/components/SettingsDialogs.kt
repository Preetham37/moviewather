package com.example.moviewather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.backup.DuplicateStrategy
import com.example.moviewather.data.backup.ImportPreviewData
import com.example.moviewather.data.backup.ImportResultSummary
import com.example.moviewather.data.preference.AppTheme

/**
 * Dialog allowing the user to select between System Default, Light, and Dark themes.
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(theme)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = {
                                onThemeSelected(theme)
                                onDismiss()
                            },
                        )
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Confirmation dialog before permanently clearing all movie and category data.
 */
@Composable
fun ClearAllDataDialog(
    movieCount: Int,
    categoryCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val movieLabel = if (movieCount == 1) "1 movie" else "$movieCount movies"
    val categoryLabel = if (categoryCount == 1) "1 category" else "$categoryCount categories"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete all movie data?",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "This will permanently remove:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• $movieLabel\n• $categoryLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete Everything",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Inspection and confirmation dialog prior to executing a backup import.
 */
@Composable
fun ImportPreviewDialog(
    previewData: ImportPreviewData,
    onConfirm: (DuplicateStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedStrategy by remember { mutableStateOf(DuplicateStrategy.SKIP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Movies",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val movieLabel = if (previewData.totalMoviesInBackup == 1) "1 movie" else "${previewData.totalMoviesInBackup} movies"
                val catLabel = if (previewData.totalCategoriesInBackup == 1) "1 category" else "${previewData.totalCategoriesInBackup} categories"

                Text(
                    text = "$movieLabel\n$catLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "New movies",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${previewData.newMoviesCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Already in collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${previewData.duplicateMoviesCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${previewData.totalCategoriesInBackup}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (previewData.duplicateMoviesCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Duplicate movies found (${previewData.duplicateMoviesCount}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    DuplicateStrategy.entries.forEach { strategy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStrategy = strategy }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedStrategy == strategy,
                                onClick = { selectedStrategy = strategy },
                            )
                            Text(
                                text = strategy.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedStrategy) }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Displays summary after a successful backup import.
 */
@Composable
fun ImportSuccessDialog(
    summary: ImportResultSummary,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import complete",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                if (summary.newMoviesAdded > 0) {
                    Text(
                        text = "• ${summary.newMoviesAdded} new movies added",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (summary.moviesReplaced > 0) {
                    Text(
                        text = "• ${summary.moviesReplaced} existing movies updated",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (summary.existingMoviesSkipped > 0) {
                    Text(
                        text = "• ${summary.existingMoviesSkipped} existing movies skipped",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (summary.categoriesRestored > 0) {
                    Text(
                        text = "• ${summary.categoriesRestored} categories restored",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (summary.relationshipsRestored > 0) {
                    Text(
                        text = "• ${summary.relationshipsRestored} category assignments restored",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

/**
 * Displays summary after a successful backup export.
 */
@Composable
fun ExportSuccessDialog(
    movieCount: Int,
    categoryCount: Int,
    onDismiss: () -> Unit,
) {
    val movieLabel = if (movieCount == 1) "1 movie" else "$movieCount movies"
    val catLabel = if (categoryCount == 1) "1 category" else "$categoryCount categories"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export successful",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = "Movie collection exported successfully.\n\n$movieLabel\n$catLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

/**
 * Dialog to view or configure an optional TMDb API key.
 */
@Composable
fun TmdbApiKeyDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "TMDb API Key",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "Configure an optional TMDb (The Movie Database) API key for real-time online movie identification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Paste TMDb API key here") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When configured, online movie information and posters will be fetched from TMDb when searching.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveKey(keyInput.trim())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

