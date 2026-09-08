package com.example.contextos.capture.modules

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.example.contextos.models.ContextItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basic metadata representation of an installed or active app.
 */
data class AppMetadata(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val lastTimeUsed: Long = 0L
)

/**
 * Capture module for installed and actively running applications.
 *
 * =========================================================================================
 * ANDROID OS ARCHITECTURAL RESTRICTIONS & LEGAL COMPLIANCE
 * =========================================================================================
 * Android's security model enforces process sandboxing via distinct Linux UIDs and SELinux
 * policies. Consequently:
 *
 * 1. UNRESTRICTED APP INSPECTION IS IMPOSSIBLE & PROHIBITED:
 *    - Ordinary applications cannot inspect the internal UI hierarchy, activity backstacks,
 *      DOM elements, network traffic, or in-memory state of third-party applications without
 *      root privileges or specialized enterprise Device Owner policies.
 *    - Deprecated APIs such as ActivityManager.getRunningTasks() and
 *      ActivityManager.getRunningAppProcesses() were intentionally restricted by Google starting
 *      in Android 5.0 (Lollipop) / 5.1 and now only return the caller's own process.
 *
 * 2. LEGAL & RELIABLE ALTERNATIVES IMPLEMENTED HERE:
 *    - UsageStatsManager: The official Android system service for inspecting foreground app usage
 *      timestamps over recent time intervals. Requires the special permission
 *      `android.permission.PACKAGE_USAGE_STATS` granted explicitly by the user in
 *      Settings > Special App Access > Usage Access.
 *    - PackageManager Launcher Queries: Queries apps explicitly installed and registered with
 *      `Intent.CATEGORY_LAUNCHER`, enabling the user to confirm their workspace composition
 *      and launch them reliably via Intents during context restoration.
 * =========================================================================================
 */
@Singleton
class AppCaptureModule @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Checks if the user has granted Special App Access for Usage Statistics.
     */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            @Suppress("DEPRECATION")
            val mode = try {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } catch (e: NoSuchMethodError) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Inspects recent app usage if permitted, filtering out system background processes
     * and self, ordered by most recently active.
     */
    fun captureRecentApps(
        snapshotId: String,
        lookbackMillis: Long = 1000L * 60 * 60 * 6, // past 6 hours
        limit: Int = 8
    ): List<ContextItem> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - lookbackMillis

        val usageStatsList: List<UsageStats> = try {
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
        } catch (e: Exception) {
            emptyList()
        }

        if (usageStatsList.isEmpty()) return emptyList()

        val packageManager = context.packageManager
        val selfPackage = context.packageName

        val recentUsages = usageStatsList
            .filter { it.lastTimeUsed > startTime && it.packageName != selfPackage }
            .sortedByDescending { it.lastTimeUsed }

        val captured = mutableListOf<ContextItem>()
        val seenPackages = mutableSetOf<String>()

        for (usage in recentUsages) {
            if (captured.size >= limit) break
            val pkg = usage.packageName
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val appLabel = getAppName(pkg, packageManager) ?: continue

            // Determine split-screen candidate (first two primary apps)
            val isPrimary = captured.size < 2

            captured.add(
                ContextItem.createApp(
                    snapshotId = snapshotId,
                    packageName = pkg,
                    appName = appLabel,
                    launchIntentUri = packageManager.getLaunchIntentForPackage(pkg)?.toUri(0),
                    isPrimaryForSplitScreen = isPrimary,
                    displayOrder = captured.size
                )
            )
        }

        return captured
    }

    /**
     * Retrieves all installed apps with a launcher activity so the user can easily
     * pick or verify their work apps.
     */
    fun getInstalledLauncherApps(): List<AppMetadata> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val apps = mutableListOf<AppMetadata>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val appName = resolveInfo.loadLabel(packageManager).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            apps.add(AppMetadata(packageName = pkg, appName = appName, isSystemApp = isSystem))
        }

        return apps.sortedBy { it.appName.lowercase() }
    }

    fun createAppItem(
        snapshotId: String,
        packageName: String,
        appName: String,
        launchIntentUri: String? = null,
        isPrimaryForSplitScreen: Boolean = false,
        displayOrder: Int = 0
    ): ContextItem {
        return ContextItem.createApp(
            snapshotId = snapshotId,
            packageName = packageName,
            appName = appName,
            launchIntentUri = launchIntentUri ?: context.packageManager.getLaunchIntentForPackage(packageName)?.toUri(0),
            isPrimaryForSplitScreen = isPrimaryForSplitScreen,
            displayOrder = displayOrder
        )
    }

    private fun getAppName(packageName: String, packageManager: PackageManager): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            null
        }
    }
}
