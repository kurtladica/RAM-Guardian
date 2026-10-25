package com.example.data.apps

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppFilterType {
    ALL,
    USER,
    SYSTEM
}

enum class AppSortType(val label: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    RECENTLY_USED("Recently Used"),
    INSTALL_DATE("Install Date (Newest)")
}

class AppRepository(
    private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun getInstalledApps(): Result<List<AppInfoModel>> = withContext(Dispatchers.IO) {
        try {
            val usageMap = if (hasUsageAccessPermission()) {
                fetchUsageStats()
            } else {
                emptyMap()
            }

            val packages: List<PackageInfo> = try {
                packageManager.getInstalledPackages(0)
            } catch (e: Exception) {
                // Fallback to querying launchable activities if getInstalledPackages is restricted
                emptyList()
            }

            val appList = mutableListOf<AppInfoModel>()

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo
                val isSystem = (appInfo != null) && ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                val isUpdatedSystem = (appInfo != null) && ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                val label = try {
                    appInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
                } catch (_: Exception) {
                    pkg.packageName
                }

                val hasLaunchIntent = try {
                    packageManager.getLaunchIntentForPackage(pkg.packageName) != null
                } catch (_: Exception) {
                    false
                }

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && appInfo != null) {
                    appInfo.minSdkVersion
                } else {
                    24
                }

                val category = if (appInfo != null) {
                    AppInfoModel.resolveCategory(appInfo.category)
                } else {
                    "Application"
                }

                val usageStat = usageMap[pkg.packageName]

                appList.add(
                    AppInfoModel(
                        packageName = pkg.packageName,
                        name = if (label.isNotBlank()) label else pkg.packageName,
                        isSystemApp = isSystem && !isUpdatedSystem,
                        isUpdatedSystemApp = isUpdatedSystem,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = versionCode,
                        minSdkVersion = minSdkVersion,
                        targetSdkVersion = appInfo?.targetSdkVersion ?: 0,
                        installTimeMillis = pkg.firstInstallTime,
                        lastUpdateTimeMillis = pkg.lastUpdateTime,
                        categoryName = category,
                        hasLaunchIntent = hasLaunchIntent,
                        lastTimeUsedMillis = usageStat?.first,
                        foregroundTimeMillis24h = usageStat?.second
                    )
                )
            }

            Result.success(appList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchUsageStats(): Map<String, Pair<Long, Long>> {
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000L) // Last 24 hours

        return try {
            val statsList = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: return emptyMap()

            statsList.associate { stat ->
                stat.packageName to Pair(stat.lastTimeUsed, stat.totalTimeInForeground)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun filterApps(
            apps: List<AppInfoModel>,
            query: String,
            filter: AppFilterType
        ): List<AppInfoModel> {
            val trimmedQuery = query.trim().lowercase()
            return apps.filter { app ->
                val matchesFilter = when (filter) {
                    AppFilterType.ALL -> true
                    AppFilterType.USER -> !app.isSystemApp || app.isUpdatedSystemApp
                    AppFilterType.SYSTEM -> app.isSystemApp && !app.isUpdatedSystemApp
                }

                val matchesQuery = if (trimmedQuery.isEmpty()) {
                    true
                } else {
                    app.name.lowercase().contains(trimmedQuery) ||
                        app.packageName.lowercase().contains(trimmedQuery)
                }

                matchesFilter && matchesQuery
            }
        }

        fun sortApps(
            apps: List<AppInfoModel>,
            sort: AppSortType
        ): List<AppInfoModel> {
            return when (sort) {
                AppSortType.NAME_ASC -> apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                AppSortType.NAME_DESC -> apps.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
                AppSortType.RECENTLY_USED -> apps.sortedWith(
                    compareByDescending<AppInfoModel> { it.lastTimeUsedMillis ?: 0L }
                        .thenByDescending { it.lastUpdateTimeMillis }
                )
                AppSortType.INSTALL_DATE -> apps.sortedByDescending { it.installTimeMillis }
            }
        }
    }
}
