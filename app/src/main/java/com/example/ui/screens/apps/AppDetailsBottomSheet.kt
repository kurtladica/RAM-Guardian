package com.example.ui.screens.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppWarning
import com.example.data.apps.AppWarningClassifier
import com.example.data.apps.WarningSeverity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceContainer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsBottomSheet(
    app: AppInfoModel,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAppReturnedFromSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warning: AppWarning? = AppWarningClassifier.classify(app)

    fun launchSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onAppReturnedFromSettings()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Unable to open system settings for ${app.name}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Device manufacturer restricted settings access",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CharcoalSurface,
        contentColor = TextPrimary,
        modifier = modifier.testTag("app_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header Row: Icon + Name + Package
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconView(
                    packageName = app.packageName,
                    size = 56.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBadge(
                    label = app.appTypeLabel,
                    accentColor = if (app.isSystemApp) CyanAccent else EmeraldAccent
                )
                InfoBadge(
                    label = app.categoryName,
                    accentColor = TextSecondary
                )
                if (app.hasLaunchIntent) {
                    InfoBadge(
                        label = "Launchable",
                        accentColor = EmeraldAccent
                    )
                }
            }

            // Warning Notice if applicable (Phase 4 requirement 7)
            if (warning != null) {
                Spacer(modifier = Modifier.height(14.dp))
                val warnColor = when (warning.severity) {
                    WarningSeverity.CRITICAL -> AmberAccent
                    WarningSeverity.WARNING -> AmberAccent
                    WarningSeverity.INFO -> CyanAccent
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(warnColor.copy(alpha = 0.12f))
                        .border(1.dp, warnColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("app_force_stop_warning")
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (warning.severity == WarningSeverity.CRITICAL) Icons.Filled.Dangerous else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = warnColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = warning.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = warning.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Non-Fabricated RAM Isolation Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CharcoalSurfaceContainer)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .testTag("app_ram_isolation_card")
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Per-App RAM: Data unavailable",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Android isolates process memory across application UIDs. Cross-app per-process RAM cannot be measured without root, so values are strictly not fabricated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Application Metadata
            Text(
                text = "PACKAGE METADATA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = EmeraldAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailItemRow(
                icon = Icons.Outlined.Info,
                title = "Version",
                value = "${app.versionName} (Build ${app.versionCode})"
            )
            DetailItemRow(
                icon = Icons.Outlined.Category,
                title = "SDK Compatibility",
                value = "Target SDK ${app.targetSdkVersion} (Min SDK ${app.minSdkVersion})"
            )
            DetailItemRow(
                icon = Icons.Outlined.Schedule,
                title = "Installed Date",
                value = app.formattedInstallDate()
            )
            DetailItemRow(
                icon = Icons.Outlined.History,
                title = "Last Updated",
                value = app.formattedUpdateDate()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Usage Information
            Text(
                text = "USAGE ACTIVITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CyanAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailItemRow(
                icon = Icons.Outlined.Schedule,
                title = "Last Time Used",
                value = app.formattedLastUsed()
            )
            DetailItemRow(
                icon = Icons.Outlined.History,
                title = "Foreground Time (24h)",
                value = app.formattedForegroundTime()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Manual Force Stop Workflow Guidance (Requirement 2 & 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalSurfaceContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Manual Confirmation Required: Android requires users to press the native 'Force stop' button manually inside system settings. Opening settings does NOT automatically stop the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Force Stop Button (Phase 4 requirement 1 & 2)
            Button(
                onClick = { launchSystemSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("force_stop_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (warning?.severity == WarningSeverity.CRITICAL) AmberAccent else EmeraldAccent,
                    contentColor = CharcoalSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Open Settings to Force Stop",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InfoBadge(
    label: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = accentColor
        )
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary
        )
    }
}
