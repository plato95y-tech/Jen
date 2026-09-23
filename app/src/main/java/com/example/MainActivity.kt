package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.DebtAppNavHost
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DebtViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: DebtViewModel by viewModels()

    private var pendingShortcutAction by mutableStateOf<String?>(null)
    private var pendingStoreId by mutableStateOf(0L)
    private var pendingStoreLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode, themePalette = themePalette) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DebtAppNavHost(
                        viewModel = viewModel,
                        shortcutAction = pendingShortcutAction,
                        shortcutStoreId = pendingStoreId,
                        shortcutStoreLocked = pendingStoreLocked,
                        onShortcutHandled = {
                            pendingShortcutAction = null
                            pendingStoreId = 0L
                            pendingStoreLocked = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra("shortcut_action")
        if (action != null) {
            pendingShortcutAction = action
            pendingStoreId = intent.getLongExtra("extra_store_id", 0L)
            pendingStoreLocked = intent.getBooleanExtra("extra_store_locked", false)
        }
    }
}


