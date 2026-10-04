package com.example.moviewather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.util.DuplicateCheckResult

/**
 * Dialog alerting the user to an exact duplicate, release year match, or similar movie in their collection.
 *
 * Adheres strictly to the duplicate hierarchy in Section 9:
 * - External ID match: Very strong duplicate
 * - Title + Year match: Strong duplicate
 * - Title only match: Possible duplicate
 * - Similar title: Warning only
 */
@Composable
fun DuplicateWarningDialog(
    checkResult: DuplicateCheckResult,
    candidateTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
) {
    when (checkResult) {
        is DuplicateCheckResult.ExternalIdMatch -> {
            val statusLabel = if (checkResult.isWatched) "Watched" else "Watchlist"
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Exact Movie Already in Collection",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "This exact movie (\"${checkResult.existingMovie.title}\"${if (checkResult.existingMovie.releaseYear != null) " - ${checkResult.existingMovie.releaseYear}" else ""}) has already been identified and saved.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Collection: $statusLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Do you want to add another copy anyway?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(confirmLabel ?: "Add Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
            )
        }

        is DuplicateCheckResult.ExactTitleAndYearMatch -> {
            val statusLabel = if (checkResult.isWatched) "Watched" else "Watchlist"
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Same Movie and Year Exists",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "\"${checkResult.existingMovie.title}\" (${checkResult.releaseYear}) is already in your collection.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Status: $statusLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Do you want to add this movie anyway?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(confirmLabel ?: "Add Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
            )
        }

        is DuplicateCheckResult.ExactMatch -> {
            val statusLabel = if (checkResult.isWatched) "Watched" else "Watchlist"
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = if (checkResult.isWatched) "Already in your collection" else "Movie already exists",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "\"${checkResult.existingMovie.title}\" is already in your movie collection.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Status: $statusLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Do you want to save this anyway?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(confirmLabel ?: "Save Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
            )
        }

        is DuplicateCheckResult.SimilarMatch -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Similar movie found",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "You already have:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "\"${checkResult.existingMovie.title}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You entered:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "\"$candidateTitle\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = checkResult.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "These may be different movies. Add anyway?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(confirmLabel ?: "Add Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
            )
        }

        DuplicateCheckResult.None -> Unit
    }
}
