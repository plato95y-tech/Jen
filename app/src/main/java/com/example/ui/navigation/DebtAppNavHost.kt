package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoreDetailScreen
import com.example.ui.viewmodel.DebtViewModel

@Composable
fun DebtAppNavHost(
    viewModel: DebtViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    shortcutAction: String? = null,
    shortcutStoreId: Long = 0L,
    shortcutStoreLocked: Boolean = false,
    onShortcutHandled: () -> Unit = {}
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    // Handle App Shortcut / Widget Intent navigation
    androidx.compose.runtime.LaunchedEffect(shortcutAction, isUnlocked, isAppLockEnabled) {
        if (shortcutAction != null && (!isAppLockEnabled || isUnlocked)) {
            when (shortcutAction) {
                "add_debt" -> {
                    navController.navigate(
                        Screen.AddTransaction.createRoute(
                            storeId = shortcutStoreId,
                            type = "DEBT",
                            txId = 0L,
                            locked = shortcutStoreLocked
                        )
                    )
                    onShortcutHandled()
                }
                "add_payment" -> {
                    navController.navigate(
                        Screen.AddTransaction.createRoute(
                            storeId = shortcutStoreId,
                            type = "PAYMENT",
                            txId = 0L,
                            locked = shortcutStoreLocked
                        )
                    )
                    onShortcutHandled()
                }
                "reports" -> {
                    navController.navigate(Screen.Reports.route)
                    onShortcutHandled()
                }
            }
        }
    }

    // Force RTL layout direction across all screens for authentic, polished Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (isAppLockEnabled && !isUnlocked) {
            LockScreen(
                viewModel = viewModel,
                modifier = modifier.fillMaxSize()
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = modifier
            ) {
            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToStore = { storeId ->
                        navController.navigate(Screen.StoreDetail.createRoute(storeId))
                    },
                    onNavigateToAddTransaction = { storeId, type, locked ->
                        navController.navigate(Screen.AddTransaction.createRoute(storeId, type, 0L, locked = locked))
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onNavigateToBackup = {
                        navController.navigate(Screen.Backup.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // Store Detail Screen
            composable(
                route = Screen.StoreDetail.route,
                arguments = listOf(navArgument("storeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getLong("storeId") ?: 0L
                StoreDetailScreen(
                    storeId = storeId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddTransaction = { sId, type, txId ->
                        navController.navigate(Screen.AddTransaction.createRoute(sId, type, txId, locked = true))
                    }
                )
            }

            // Add or Edit Transaction Screen
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("storeId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "DEBT"
                    },
                    navArgument("txId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("locked") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val storeId = backStackEntry.arguments?.getLong("storeId") ?: 0L
                val type = backStackEntry.arguments?.getString("type") ?: "DEBT"
                val txId = backStackEntry.arguments?.getLong("txId") ?: 0L
                val isLocked = backStackEntry.arguments?.getBoolean("locked") ?: false
                AddEditTransactionScreen(
                    initialStoreId = storeId,
                    initialTypeString = type,
                    txId = txId,
                    isStoreLocked = isLocked,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Reports Screen
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStore = { storeId ->
                        navController.navigate(Screen.StoreDetail.createRoute(storeId))
                    }
                )
            }

            // Backup & Restore Screen
            composable(Screen.Backup.route) {
                BackupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
