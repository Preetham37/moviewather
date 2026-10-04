package com.example.moviewather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Dialog to create a new category with non-empty validation and duplicate checking.
 */
@Composable
fun CreateCategoryDialog(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
    onCategoryCreated: ((Category) -> Unit)? = null,
) {
    var nameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var duplicateConflictName by remember { mutableStateOf<String?>(null) }

    if (duplicateConflictName != null) {
        AlertDialog(
            onDismissRequest = { duplicateConflictName = null },
            title = {
                Text(
                    text = "Category already exists",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "You already have a category named\n\"$duplicateConflictName\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { duplicateConflictName = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Category",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Category name") },
                    placeholder = { Text("Must Watch") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = nameInput.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Category name cannot be empty"
                        return@TextButton
                    }

                    val duplicate = viewModel.findDuplicateCategory(trimmed)
                    if (duplicate != null) {
                        duplicateConflictName = duplicate.name
                        return@TextButton
                    }

                    viewModel.createCategory(trimmed)
                    onDismiss()
                }
            ) {
                Text("Create")
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
 * Dialog to rename an existing category with duplicate prevention.
 */
@Composable
fun EditCategoryDialog(
    category: Category,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
) {
    var nameInput by remember(category.name) { mutableStateOf(category.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var duplicateConflictName by remember { mutableStateOf<String?>(null) }

    if (duplicateConflictName != null) {
        AlertDialog(
            onDismissRequest = { duplicateConflictName = null },
            title = {
                Text(
                    text = "Category already exists",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "You already have a category named\n\"$duplicateConflictName\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { duplicateConflictName = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Category",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Category name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = nameInput.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Category name cannot be empty"
                        return@TextButton
                    }
                    if (trimmed == category.name.trim()) {
                        onDismiss()
                        return@TextButton
                    }

                    val duplicate = viewModel.findDuplicateCategory(trimmed, excludeCategoryId = category.id)
                    if (duplicate != null) {
                        duplicateConflictName = duplicate.name
                        return@TextButton
                    }

                    viewModel.updateCategoryName(category, trimmed)
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

/**
 * Confirmation dialog shown before deleting a category.
 * Explicitly reassures the user that their movies will NOT be deleted.
 */
@Composable
fun DeleteCategoryDialog(
    category: Category,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete category?",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "\"${category.name}\" will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your movies will NOT be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
