package com.example.contextos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.contextos.ui.contextlist.ContextListScreen
import com.example.contextos.ui.contextlist.ContextListViewModel
import com.example.contextos.ui.detail.ContextDetailScreen
import com.example.contextos.ui.detail.ContextDetailViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                val viewModel: ContextListViewModel = hiltViewModel()
                ContextListScreen(
                    viewModel = viewModel,
                    onSnapshotClick = { snapshot ->
                        backStack.add(ContextDetailNavKey(snapshot.id))
                    },
                    onVoiceClick = {
                        // Voice trigger callback (for Phase 3)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<ContextDetailNavKey> { key ->
                val viewModel: ContextDetailViewModel = hiltViewModel()
                ContextDetailScreen(
                    snapshotId = key.snapshotId,
                    viewModel = viewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onResumeClick = { snapshot ->
                        // Restoration trigger callback
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
