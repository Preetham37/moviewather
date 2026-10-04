package com.example.moviewather.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moviewather.data.backup.BackupManager
import com.example.moviewather.data.backup.BackupValidationResult
import com.example.moviewather.data.backup.DuplicateStrategy
import com.example.moviewather.data.backup.ImportPreviewData
import com.example.moviewather.data.backup.ImportResultSummary
import com.example.moviewather.ui.components.ClearAllDataDialog
import com.example.moviewather.ui.components.ExportSuccessDialog
import com.example.moviewather.ui.components.ImportPreviewDialog
import com.example.moviewather.ui.components.ImportSuccessDialog
import com.example.moviewather.ui.components.ThemeSelectionDialog
import com.example.moviewather.ui.components.TmdbApiKeyDialog
import com.example.moviewather.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen providing Appearance preferences, offline JSON Data backup/restore,
 * data clearance, and application information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MovieViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTheme by viewModel.currentTheme.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()

    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var pendingImportPreview by remember { mutableStateOf<ImportPreviewData?>(null) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var importSuccessSummary by remember { mutableStateOf<ImportResultSummary?>(null) }
    var exportSuccessCounts by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // SAF Launcher for Exporting backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = viewModel.generateBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                        }
                    }
                    exportSuccessCounts = allMovies.size to allCategories.size
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    // SAF Launcher for Importing backup
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                            it.readText()
                        }
                    } ?: ""

                    when (val result = BackupManager.parseAndValidate(jsonContent)) {
                        is BackupValidationResult.Error -> {
                            importErrorMessage = result.message
                        }
                        is BackupValidationResult.Success -> {
                            val preview = viewModel.createImportPreview(result.backup)
                            pendingImportPreview = preview
                        }
                    }
                } catch (e: Exception) {
                    importErrorMessage = "Unable to read the selected backup file."
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ── 1. Appearance Section ─────────────────────────────────────────
            SettingsSectionHeader(title = "Appearance")

            SettingsActionRow(
                icon = Icons.Outlined.DarkMode,
                title = "Theme",
                subtitle = currentTheme.label,
                onClick = { showThemeDialog = true },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ── 2. Data Section ───────────────────────────────────────────────
            SettingsSectionHeader(title = "Data")

            SettingsActionRow(
                icon = Icons.Outlined.Upload,
                title = "Export Movies",
                subtitle = "Save a JSON backup of movies and categories",
                onClick = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("movie-watcher-backup-$dateStr.json")
                },
            )

            SettingsActionRow(
                icon = Icons.Outlined.Download,
                title = "Import Movies",
                subtitle = "Restore a Movie Watcher backup file",
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
            )

            SettingsActionRow(
                icon = Icons.Outlined.Key,
                title = "TMDb API Key",
                subtitle = if (tmdbApiKey.isNotBlank()) {
                    "Configured (${tmdbApiKey.take(4)}...${tmdbApiKey.takeLast(4)})"
                } else if (com.example.moviewather.BuildConfig.TMDB_API_KEY.isNotBlank()) {
                    "Configured via Build"
                } else {
                    "Optional: tap to configure for live online identification"
                },
                onClick = { showApiKeyDialog = true },
            )

            SettingsActionRow(
                icon = Icons.Outlined.CleaningServices,
                title = "Clear All Data",
                subtitle = "Permanently remove all movies and categories",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showClearDataDialog = true },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ── 3. About Section ──────────────────────────────────────────────
            SettingsSectionHeader(title = "About")

            SettingsActionRow(
                icon = Icons.Outlined.Info,
                title = "Movie Watcher",
                subtitle = "Version 1.0\n\nA simple personal movie watchlist designed to help you save and find the movies you want to watch.",
                onClick = {},
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { newTheme -> viewModel.setTheme(newTheme) },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showClearDataDialog) {
        ClearAllDataDialog(
            movieCount = allMovies.size,
            categoryCount = allCategories.size,
            onConfirm = {
                showClearDataDialog = false
                coroutineScope.launch {
                    viewModel.clearAllData()
                    snackbarHostState.showSnackbar("All movie data has been cleared.")
                }
            },
            onDismiss = { showClearDataDialog = false },
        )
    }

    if (showApiKeyDialog) {
        TmdbApiKeyDialog(
            currentApiKey = tmdbApiKey,
            onSaveKey = { newKey -> viewModel.setTmdbApiKey(newKey) },
            onDismiss = { showApiKeyDialog = false },
        )
    }

    pendingImportPreview?.let { preview ->
        ImportPreviewDialog(
            previewData = preview,
            onConfirm = { strategy: DuplicateStrategy ->
                pendingImportPreview = null
                coroutineScope.launch {
                    try {
                        val summary = viewModel.executeImport(preview.backup, strategy)
                        importSuccessSummary = summary
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Import failed: ${e.localizedMessage ?: "Unknown error"}")
                    }
                }
            },
            onDismiss = { pendingImportPreview = null },
        )
    }

    importErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            title = {
                Text(
                    text = "Unable to import",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { importErrorMessage = null }) {
                    Text("OK")
                }
            },
        )
    }

    importSuccessSummary?.let { summary ->
        ImportSuccessDialog(
            summary = summary,
            onDismiss = { importSuccessSummary = null },
        )
    }

    exportSuccessCounts?.let { (movieCount, categoryCount) ->
        ExportSuccessDialog(
            movieCount = movieCount,
            categoryCount = categoryCount,
            onDismiss = { exportSuccessCounts = null },
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
