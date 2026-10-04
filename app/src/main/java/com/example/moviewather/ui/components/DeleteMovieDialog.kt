package com.example.moviewather.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.moviewather.data.local.entity.Movie

/**
 * Confirmation dialog shown before permanently deleting a movie.
 *
 * @param movie The movie to be deleted.
 * @param onConfirm Callback when user confirms permanent deletion.
 * @param onDismiss Callback when user cancels or dismisses the dialog.
 */
@Composable
fun DeleteMovieDialog(
    movie: Movie,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete movie?",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = "\"${movie.title}\" will be permanently removed from your movie collection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
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
