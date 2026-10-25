package com.example.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TouchApp
import com.example.ui.screens.automation.AccessibilityConsentDialog
import com.example.ui.screens.automation.ProtectedAppsDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.settings.AppThemeMode
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceContainer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Screen 7: Settings
 * Redesigned to match Mockup Screen 7 with grouped section cards and clean layout.
 */
@Composable
fun SettingsScreen(
    onNavigateToUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var autoRefreshEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        viewModel.refreshUsagePermissionState()
        onDispose { }
    }

    LaunchedEffect(uiState.actionFeedbackMessage) {
        uiState.actionFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header: Title + Subtitle
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Preferences & System Integrity",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            // Section 1: DASHBOARD
            item {
                SectionHeader(title = "DASHBOARD")
                Spacer(modifier = Modifier.height(8.dp))
                DashboardSectionCard(
                    refreshIntervalMillis = uiState.refreshIntervalMillis,
                    autoRefreshEnabled = autoRefreshEnabled,
                    onSelectInterval = { viewModel.setRefreshInterval(it) },
                    onToggleAutoRefresh = { autoRefreshEnabled = it }
                )
            }

            // Section 2: APPEARANCE
            item {
                SectionHeader(title = "APPEARANCE")
                Spacer(modifier = Modifier.height(8.dp))
                AppearanceSectionCard(
                    currentTheme = uiState.themeMode,
                    onSelectTheme = { viewModel.setThemeMode(it) }
                )
            }

            // Section 3: PERMISSIONS
            item {
                SectionHeader(title = "PERMISSIONS")
                Spacer(modifier = Modifier.height(8.dp))
                PermissionsSectionCard(
                    isGranted = uiState.hasUsageAccess,
                    onOpenUsageAccessScreen = onNavigateToUsageAccess
                )
            }

            // Section 4: ACCESSIBILITY AUTOMATION
            item {
                SectionHeader(title = "ACCESSIBILITY AUTOMATION")
                Spacer(modifier = Modifier.height(8.dp))
                AccessibilityAutomationSectionCard(
                    isServiceConnected = uiState.isAccessibilityConnected,
                    isServiceEnabled = uiState.isAccessibilityEnabled,
                    automationDelayMillis = uiState.automationDelayMillis,
                    userWhitelistCount = uiState.userWhitelist.size,
                    onOpenConfigure = { viewModel.showConsentDialog() },
                    onOpenProtectedApps = { viewModel.showProtectedAppsDialog() },
                    onSelectDelay = { viewModel.setAutomationDelay(it) }
                )
            }

            // Section 5: DATA & PRIVACY
            item {
                SectionHeader(title = "DATA & PRIVACY")
                Spacer(modifier = Modifier.height(8.dp))
                DataAndPrivacySectionCard(
                    onClearHistory = { viewModel.promptClearHistory() },
                    onResetDefaults = { viewModel.promptResetDefaults() }
                )
            }

            // Section 6: ABOUT
            item {
                SectionHeader(title = "ABOUT RAM GUARDIAN")
                Spacer(modifier = Modifier.height(8.dp))
                AboutSectionCard()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Clear History Dialog
        if (uiState.showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearHistory() },
                title = { Text("Clear RAM Graph History?") },
                text = { Text("This will reset the bounded 20-measurement live history recorded in the Dashboard graph during this session.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmClearHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Clear History")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissClearHistory() }) {
                        Text("Cancel")
                    }
                },
                containerColor = CharcoalSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }

        // Reset Preferences Dialog
        if (uiState.showResetDefaultsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResetDefaults() },
                title = { Text("Reset Preferences to Defaults?") },
                text = { Text("This will revert the theme to Dark, restore the refresh interval to 2.0 seconds, and reset local graph history.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmResetDefaults() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Reset All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissResetDefaults() }) {
                        Text("Cancel")
                    }
                },
                containerColor = CharcoalSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }

        // Accessibility Service Consent Dialog
        if (uiState.showConsentDialog) {
            AccessibilityConsentDialog(
                onDismiss = { viewModel.dismissConsentDialog() },
                onConsentGranted = { viewModel.dismissConsentDialog() }
            )
        }

        // Protected Apps & User Whitelist Dialog
        if (uiState.showProtectedAppsDialog) {
            ProtectedAppsDialog(
                userWhitelist = uiState.userWhitelist,
                onRemoveFromWhitelist = { viewModel.removeFromWhitelist(it) },
                onDismiss = { viewModel.dismissProtectedAppsDialog() }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 11.sp
        ),
        color = TextTertiary
    )
}

@Composable
private fun DashboardSectionCard(
    refreshIntervalMillis: Long,
    autoRefreshEnabled: Boolean,
    onSelectInterval: (Long) -> Unit,
    onToggleAutoRefresh: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("setting_refresh_interval_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Auto refresh toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-Refresh Telemetry",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Poll ActivityManager while Dashboard is visible",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = autoRefreshEnabled,
                    onCheckedChange = onToggleAutoRefresh,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CharcoalBackground,
                        checkedTrackColor = EmeraldAccent,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = CharcoalSurfaceContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Refresh Cadence chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Refresh Cadence",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf(
                        1000L to "1s (Fast)",
                        2000L to "2s (Default)",
                        5000L to "5s (Eco)"
                    )

                    intervals.forEach { (millis, label) ->
                        val isSelected = refreshIntervalMillis == millis
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectInterval(millis) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CharcoalSurfaceContainer,
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = OnEmeraldContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = EmeraldAccent.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("interval_chip_${millis}ms")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSectionCard(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("setting_theme_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Choose interface theme mode",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = currentTheme == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTheme(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CharcoalSurfaceContainer,
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = OnEmeraldContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderSubtle,
                            selectedBorderColor = EmeraldAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("theme_chip_${mode.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionsSectionCard(
    isGranted: Boolean,
    onOpenUsageAccessScreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenUsageAccessScreen)
            .padding(18.dp)
            .testTag("setting_usage_access_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SkyAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = SkyAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Usage Access",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isGranted) "Permission Granted · Tap to manage" else "Optional · Not granted",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isGranted) EmeraldAccent else AmberAccent
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DataAndPrivacySectionCard(
    onClearHistory: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("setting_privacy_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Zero-Network Policy",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "100% on-device processing · No cloud telemetry",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "RAM Guardian contains zero network permissions. No analytics, tracking SDKs, or cloud backends exist. All memory telemetry is computed and stored locally in volatile RAM.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear History", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }

                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Defaults", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }
            }
        }
    }
}

@Composable
private fun AboutSectionCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("setting_about_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "RAM Guardian",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary
                    )
                }
                Text(
                    text = "v1.3.0",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = EmeraldAccent
                )
            }

            Text(
                text = "Native Android no-root RAM monitoring and app-management utility. Built with Jetpack Compose & Material 3.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )

            Text(
                text = "Honest Platform Architecture & Limitations:\n• Ordinary non-root apps cannot kill arbitrary background processes directly; all force-stop actions route transparently through native Android Settings.\n• Android isolates cross-app memory pages for security; per-app RAM values are not fabricated.\n• Android intentionally keeps dormant apps cached in RAM to accelerate launch speed.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun AccessibilityAutomationSectionCard(
    isServiceConnected: Boolean,
    isServiceEnabled: Boolean,
    automationDelayMillis: Long,
    userWhitelistCount: Int,
    onOpenConfigure: () -> Unit,
    onOpenProtectedApps: () -> Unit,
    onSelectDelay: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("setting_accessibility_automation_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Service Connection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isServiceConnected) EmeraldAccent.copy(alpha = 0.12f)
                                else AmberAccent.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = null,
                            tint = if (isServiceConnected) EmeraldAccent else AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Force-Stop Automation",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isServiceConnected) "Service Connected & Ready"
                            else if (isServiceEnabled) "Service Enabled (Connecting)"
                            else "Service Disabled (Tap to enable)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isServiceConnected) EmeraldAccent else AmberAccent
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isServiceConnected) EmeraldAccent.copy(alpha = 0.15f)
                            else AmberAccent.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isServiceConnected) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (isServiceConnected) EmeraldAccent else AmberAccent
                    )
                }
            }

            Text(
                text = "Automates pressing the native Force stop button in Android Settings for apps you explicitly select in the Apps manager. Operates strictly when you initiate a batch run.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = TextSecondary
            )

            // Configure Button
            Button(
                onClick = onOpenConfigure,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("configure_accessibility_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceConnected) CharcoalSurfaceContainer else EmeraldAccent,
                    contentColor = if (isServiceConnected) TextPrimary else CharcoalBackground
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isServiceConnected) "Manage in Android Settings" else "Configure Accessibility Service",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Protected Apps Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalSurfaceContainer)
                    .clickable(onClick = onOpenProtectedApps)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Protected Apps & Whitelist",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "$userWhitelistCount custom whitelisted · Core system protected",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Safety Delay Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Inter-App Automation Delay",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1000L to "1.0s (Fast)",
                        1500L to "1.5s (Default)",
                        2000L to "2.0s (Safe)",
                        3000L to "3.0s (Eco)"
                    ).forEach { (millis, label) ->
                        val isSelected = automationDelayMillis == millis
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDelay(millis) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CharcoalSurfaceContainer,
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = OnEmeraldContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = EmeraldAccent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}
