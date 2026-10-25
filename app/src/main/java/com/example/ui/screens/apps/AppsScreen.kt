package com.example.ui.screens.apps

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.automation.AccessibilityUtils
import com.example.automation.BatchAutomationEngine
import com.example.automation.GuardianAccessibilityService
import com.example.data.settings.SettingsRepository
import com.example.ui.screens.automation.AccessibilityConsentDialog
import com.example.ui.screens.automation.BatchProgressScreen
import com.example.ui.screens.automation.BatchReportScreen
import com.example.ui.screens.automation.BatchReviewDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.apps.AppFilterType
import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppSortType
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceContainer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AppsScreen(
    onAppClick: (AppInfoModel) -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showBulkQueueDialog by remember { mutableStateOf(false) }
    var showBatchReviewDialog by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var showBatchReport by remember { mutableStateOf(false) }

    val batchProgress by BatchAutomationEngine.progress.collectAsStateWithLifecycle()
    val isA11yConnected by GuardianAccessibilityService.isConnected.collectAsStateWithLifecycle()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val automationDelay by settingsRepo.automationDelayMillis.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.refreshUsagePermission()
        onDispose { }
    }

    // Active Batch Automation Overlays
    if (batchProgress.isRunning) {
        BatchProgressScreen(
            progress = batchProgress,
            onStopBatch = { BatchAutomationEngine.stopBatch() }
        )
        return
    }

    if (showBatchReport && batchProgress.isFinished) {
        BatchReportScreen(
            progress = batchProgress,
            onViewTelemetry = onNavigateToDashboard,
            onDone = {
                showBatchReport = false
                viewModel.clearSelection()
                isSelectionMode = false
                viewModel.loadInstalledApps()
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
            .testTag("apps_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mockup Screen 2: Header (Apps, Manage your installed applications, Filter icon)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Apps",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage your installed applications",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterAlt,
                            contentDescription = "Sort / Filter options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Mockup Screen 2: Search input bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input"),
                    placeholder = {
                        Text(
                            text = "Search apps or package name...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalSurface,
                        unfocusedContainerColor = CharcoalSurface,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = EmeraldAccent
                    )
                )
            }

            // Mockup Screen 2: Filter chips (All Apps, User Apps, System Apps)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MockupFilterChip(
                        label = "All Apps",
                        isSelected = uiState.selectedFilter == AppFilterType.ALL,
                        onClick = { viewModel.onFilterChanged(AppFilterType.ALL) },
                        testTag = "filter_chip_all"
                    )
                    MockupFilterChip(
                        label = "User Apps",
                        isSelected = uiState.selectedFilter == AppFilterType.USER,
                        onClick = { viewModel.onFilterChanged(AppFilterType.USER) },
                        testTag = "filter_chip_user"
                    )
                    MockupFilterChip(
                        label = "System Apps",
                        isSelected = uiState.selectedFilter == AppFilterType.SYSTEM,
                        onClick = { viewModel.onFilterChanged(AppFilterType.SYSTEM) },
                        testTag = "filter_chip_system"
                    )
                }
            }

            // Mockup Screen 2: Controls row ("Sort by: Name (A-Z) v", "Total apps: 142", "Select >")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        // Sort dropdown pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CharcoalSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { sortMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("apps_sort_control")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sort by: ${uiState.selectedSort.label}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier
                                .background(CharcoalSurfaceContainer)
                                .border(1.dp, BorderMedium, RoundedCornerShape(8.dp))
                        ) {
                            AppSortType.entries.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOption.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            color = if (uiState.selectedSort == sortOption) EmeraldAccent else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.onSortChanged(sortOption)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Total apps: ${uiState.displayedApps.size}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextTertiary
                        )

                        // Select > toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    isSelectionMode = !isSelectionMode
                                    if (!isSelectionMode) viewModel.clearSelection()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSelectionMode) "Done" else "Select",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSelectionMode) EmeraldAccent else TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = if (isSelectionMode) EmeraldAccent else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Optional Usage Access Explainer (if recently used sort requested)
            if (uiState.showUsageExplainer) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalSurface)
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                            .testTag("usage_access_explainer")
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text("Usage Access Optional", style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sorting by recent activity requires Usage Access. You can grant it in Settings or sort by installation date.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = CharcoalBackground),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { viewModel.dismissUsageExplainer() }) {
                                    Text("Dismiss", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Selection Bar when in selection mode
            if (isSelectionMode && uiState.selectedPackageNames.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldContainer)
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("multi_selection_bar")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.selectedPackageNames.size} apps selected",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OnEmeraldContainer
                                )
                                Row {
                                    TextButton(onClick = { viewModel.selectAllVisible() }) {
                                        Text("Select All", color = OnEmeraldContainer, style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(onClick = { viewModel.clearSelection() }) {
                                        Text("Clear", color = OnEmeraldContainer, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showBatchReviewDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("start_force_stop_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldAccent,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Start Force Stop (${uiState.selectedPackageNames.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showBulkQueueDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnEmeraldContainer),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("start_bulk_queue_button")
                                ) {
                                    Text("Manual", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // App list states
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = EmeraldAccent, modifier = Modifier.size(32.dp))
                                Text("Loading applications...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Warning,
                            title = "Package Scan Error",
                            description = uiState.errorMessage ?: "Failed to query installed packages.",
                            testTag = "apps_error_state"
                        )
                    }
                }

                uiState.displayedApps.isEmpty() -> {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Apps,
                            title = "No Apps Found",
                            description = if (uiState.searchQuery.isNotEmpty()) "No applications match \"${uiState.searchQuery}\"" else "No applications under this filter.",
                            testTag = "apps_empty_state"
                        )
                    }
                }

                else -> {
                    items(
                        items = uiState.displayedApps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            isSelected = uiState.selectedPackageNames.contains(app.packageName),
                            isSelectionMode = isSelectionMode,
                            onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Bulk Force Stop Queue Dialog
        if (showBulkQueueDialog) {
            val queueApps = uiState.rawApps.filter { uiState.selectedPackageNames.contains(it.packageName) }
            BulkForceStopDialog(
                queue = queueApps,
                onDismiss = { showBulkQueueDialog = false },
                onQueueFinished = {
                    showBulkQueueDialog = false
                    viewModel.clearSelection()
                    isSelectionMode = false
                    viewModel.loadInstalledApps()
                }
            )
        }

        // Automated Batch Force Stop Review Dialog
        if (showBatchReviewDialog) {
            val selectedApps = uiState.rawApps.filter { uiState.selectedPackageNames.contains(it.packageName) }
            BatchReviewDialog(
                selectedApps = selectedApps,
                isAccessibilityConnected = isA11yConnected,
                currentDelayMillis = automationDelay,
                onSelectDelay = { settingsRepo.setAutomationDelay(it) },
                onStartBatch = {
                    showBatchReviewDialog = false
                    showBatchReport = true
                    BatchAutomationEngine.startBatch(
                        context = context,
                        targetApps = selectedApps,
                        delayBetweenAppsMillis = automationDelay
                    )
                },
                onRequestEnableAccessibility = {
                    showBatchReviewDialog = false
                    showConsentDialog = true
                },
                onDismiss = { showBatchReviewDialog = false }
            )
        }

        // Accessibility Service Consent & Disclosure Dialog
        if (showConsentDialog) {
            AccessibilityConsentDialog(
                onDismiss = { showConsentDialog = false },
                onConsentGranted = { showConsentDialog = false }
            )
        }
    }
}

@Composable
private fun MockupFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0F3024) else CharcoalSurface)
            .border(
                1.dp,
                if (isSelected) EmeraldAccent else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) EmeraldAccent else TextSecondary
        )
    }
}
