package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.DebtViewModel

/**
 * BackupScreen now cleanly delegates to the modernized unified SettingsScreen
 * under the dedicated BACKUP tab, ensuring a single unified source of truth for
 * backups, restoring, and auto-backup settings across the entire app.
 */
@Composable
fun BackupScreen(
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        initialTab = SettingsTab.BACKUP,
        modifier = modifier
    )
}
