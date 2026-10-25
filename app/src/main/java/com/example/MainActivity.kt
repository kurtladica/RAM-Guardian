package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.apps.AppInfoModel
import com.example.data.settings.SettingsRepository
import com.example.ui.navigation.GuardianBottomBar
import com.example.ui.navigation.NavDestination
import com.example.ui.screens.apps.AppDetailsScreen
import com.example.ui.screens.apps.AppsScreen
import com.example.ui.screens.apps.ForceStopGuideScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.optimize.BeforeAfterScreen
import com.example.ui.screens.optimize.OptimizeScreen
import com.example.ui.screens.optimize.OptimizeViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.settings.UsageAccessScreen
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.RAMGuardianTheme

sealed interface SubDestination {
    data class AppDetails(val app: AppInfoModel) : SubDestination
    data class ForceStopGuide(val app: AppInfoModel) : SubDestination
    data object BeforeAfterOptimization : SubDestination
    data object UsageAccess : SubDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository.getInstance(context) }
            val themeMode by settingsRepo.themeMode.collectAsStateWithLifecycle()

            RAMGuardianTheme(themeMode = themeMode) {
                MainAppScaffold()
            }
        }
    }
}

@Composable
fun MainAppScaffold() {
    var currentDestination by remember { mutableStateOf(NavDestination.DASHBOARD) }
    var currentSubDestination by remember { mutableStateOf<SubDestination?>(null) }

    val optimizeViewModel: OptimizeViewModel = viewModel()
    val optimizeUiState by optimizeViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // System Back Handler: manages sub-screens first, then falls back to Dashboard tab
    BackHandler(enabled = currentSubDestination != null || currentDestination != NavDestination.DASHBOARD) {
        when (val sub = currentSubDestination) {
            is SubDestination.ForceStopGuide -> {
                currentSubDestination = SubDestination.AppDetails(sub.app)
            }
            is SubDestination.AppDetails,
            is SubDestination.BeforeAfterOptimization,
            is SubDestination.UsageAccess -> {
                currentSubDestination = null
            }
            null -> {
                if (currentDestination != NavDestination.DASHBOARD) {
                    currentDestination = NavDestination.DASHBOARD
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .testTag("main_app_scaffold"),
        containerColor = CharcoalBackground,
        bottomBar = {
            if (currentSubDestination == null) {
                GuardianBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        currentSubDestination = null
                        currentDestination = destination
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentSubDestination to currentDestination,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { (subDest, mainDest) ->
                when (subDest) {
                    is SubDestination.AppDetails -> {
                        AppDetailsScreen(
                            app = subDest.app,
                            onBack = { currentSubDestination = null },
                            onNavigateToForceStopGuide = {
                                currentSubDestination = SubDestination.ForceStopGuide(subDest.app)
                            }
                        )
                    }
                    is SubDestination.ForceStopGuide -> {
                        ForceStopGuideScreen(
                            app = subDest.app,
                            onClose = {
                                currentSubDestination = SubDestination.AppDetails(subDest.app)
                            }
                        )
                    }
                    is SubDestination.BeforeAfterOptimization -> {
                        BeforeAfterScreen(
                            baseline = optimizeUiState.baselineMemory,
                            current = optimizeUiState.currentMemory,
                            comparison = optimizeUiState.comparison,
                            onCaptureBaseline = { optimizeViewModel.captureBaseline() },
                            onMeasureAgain = { optimizeViewModel.captureAfterActions() },
                            onReset = { optimizeViewModel.resetComparison() },
                            onBack = { currentSubDestination = null }
                        )
                    }
                    is SubDestination.UsageAccess -> {
                        UsageAccessScreen(
                            isGranted = settingsUiState.hasUsageAccess,
                            onBack = { currentSubDestination = null }
                        )
                    }
                    null -> {
                        when (mainDest) {
                            NavDestination.DASHBOARD -> DashboardScreen()
                            NavDestination.APPS -> AppsScreen(
                                onAppClick = { app ->
                                    currentSubDestination = SubDestination.AppDetails(app)
                                },
                                onNavigateToDashboard = {
                                    currentDestination = NavDestination.DASHBOARD
                                }
                            )
                            NavDestination.OPTIMIZE -> OptimizeScreen(
                                onNavigateToBeforeAfter = {
                                    currentSubDestination = SubDestination.BeforeAfterOptimization
                                },
                                onNavigateToApps = {
                                    currentDestination = NavDestination.APPS
                                },
                                viewModel = optimizeViewModel
                            )
                            NavDestination.SETTINGS -> SettingsScreen(
                                onNavigateToUsageAccess = {
                                    currentSubDestination = SubDestination.UsageAccess
                                },
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
