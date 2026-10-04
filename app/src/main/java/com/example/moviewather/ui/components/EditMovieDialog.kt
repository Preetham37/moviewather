package com.example.moviewather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.util.DuplicateCheckResult
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Dialog for editing movie information (Title, Release Year, Notes),
 * keeping external identification IDs intact.
 */
@Composable
fun EditMovieDialog(
    movie: Movie,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var titleInput by remember(movie.title) { mutableStateOf(movie.title) }
    var yearInput by remember(movie.releaseYear) { mutableStateOf(movie.releaseYear?.toString() ?: "") }
    var notesInput by remember(movie.notes) { mutableStateOf(movie.notes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDuplicateWarning by remember { mutableStateOf<DuplicateCheckResult?>(null) }

    fun commitChanges(newTitle: String, newYear: Int?, newNotes: String) {
        viewModel.updateMovieDetails(
            movie = movie,
            newTitle = newTitle,
            newNotes = newNotes,
            newReleaseYear = newYear,
        )
        onDismiss()
    }

    // ── Duplicate / Similarity warning dialogs ────────────────────────────────
    pendingDuplicateWarning?.let { warning ->
        DuplicateWarningDialog(
            checkResult = warning,
            candidateTitle = titleInput.trim(),
            onConfirm = {
                val titleToSave = titleInput.trim()
                val parsedYear = yearInput.trim().toIntOrNull()
                pendingDuplicateWarning = null
                commitChanges(titleToSave, parsedYear, notesInput)
            },
            onDismiss = { pendingDuplicateWarning = null },
            confirmLabel = "Save Changes",
        )
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Movie",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Movie title") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = yearInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            yearInput = it
                        }
                    },
                    label = { Text("Release Year (optional)") },
                    placeholder = { Text("e.g. 2021") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = titleInput.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Title cannot be empty"
                        return@TextButton
                    }
                    val parsedYear = yearInput.trim().toIntOrNull()
                    val titleChanged = !trimmed.equals(movie.title.trim(), ignoreCase = false)
                    val yearChanged = parsedYear != movie.releaseYear

                    if (!titleChanged && !yearChanged) {
                        commitChanges(trimmed, parsedYear, notesInput)
                        return@TextButton
                    }

                    // Check for duplicates excluding current movie
                    when (val result = viewModel.checkDuplicateForEdit(trimmed, movie.id, parsedYear, movie.externalId)) {
                        is DuplicateCheckResult.ExternalIdMatch,
                        is DuplicateCheckResult.ExactTitleAndYearMatch,
                        is DuplicateCheckResult.ExactMatch,
                        is DuplicateCheckResult.SimilarMatch -> {
                            pendingDuplicateWarning = result
                        }
                        DuplicateCheckResult.None -> {
                            commitChanges(trimmed, parsedYear, notesInput)
                        }
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
