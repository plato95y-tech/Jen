package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reports : Screen("reports")
    object Backup : Screen("backup")
    object Settings : Screen("settings")
    object StoreDetail : Screen("store/{storeId}") {
        fun createRoute(storeId: Long) = "store/$storeId"
    }
    object AddTransaction : Screen("transaction/add?storeId={storeId}&type={type}&txId={txId}&locked={locked}") {
        fun createRoute(storeId: Long = 0L, type: String = "DEBT", txId: Long = 0L, locked: Boolean = false) =
            "transaction/add?storeId=$storeId&type=$type&txId=$txId&locked=$locked"
    }
}
