package com.example.contextos.capture

import com.example.contextos.capture.modules.AppCaptureModule
import com.example.contextos.capture.modules.AppMetadata
import com.example.contextos.capture.modules.ClipboardCaptureModule
import com.example.contextos.capture.modules.DocumentCaptureModule
import com.example.contextos.capture.modules.NoteCaptureModule
import com.example.contextos.capture.modules.UrlCaptureModule
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextCaptureManagerImpl @Inject constructor(
    private val appCaptureModule: AppCaptureModule,
    private val clipboardCaptureModule: ClipboardCaptureModule,
    private val documentCaptureModule: DocumentCaptureModule,
    private val urlCaptureModule: UrlCaptureModule,
    private val noteCaptureModule: NoteCaptureModule
) : ContextCaptureManager {

    override fun hasUsageAccess(): Boolean {
        return appCaptureModule.hasUsageStatsPermission()
    }

    override fun getAvailableApps(): List<AppMetadata> {
        return appCaptureModule.getInstalledLauncherApps()
    }

    override suspend fun captureAvailableContext(
        snapshotId: String,
        includeClipboard: Boolean,
        includeRecentApps: Boolean
    ): ContextCaptureResult {
        val warnings = mutableListOf<String>()
        val detectedApps = mutableListOf<ContextItem>()

        if (includeRecentApps) {
            if (appCaptureModule.hasUsageStatsPermission()) {
                val recent = appCaptureModule.captureRecentApps(snapshotId = snapshotId, limit = 6)
                detectedApps.addAll(recent)
            } else {
                warnings.add(
                    "Usage access is not enabled. Android restrictions prevent background app discovery without " +
                            "user permission. You can enable 'Usage Access' in system settings or manually select active apps."
                )
            }
        }

        var clipboardItem: ContextItem? = null
        if (includeClipboard) {
            // Android 10+ requires window focus to read clipboard safely
            clipboardItem = try {
                clipboardCaptureModule.captureCurrentClipboard(
                    snapshotId = snapshotId,
                    displayOrder = detectedApps.size
                )
            } catch (t: Throwable) {
                null
            }
        }

        return ContextCaptureResult(
            snapshotId = snapshotId,
            detectedApps = detectedApps,
            capturedClipboard = clipboardItem,
            capturedUrls = emptyList(),
            selectedDocuments = emptyList(),
            notes = emptyList(),
            warnings = warnings
        )
    }

    override suspend fun captureCurrentContext(name: String, description: String): ContextSnapshot {
        val snapshotId = UUID.randomUUID().toString()
        val result = captureAvailableContext(
            snapshotId = snapshotId,
            includeClipboard = true,
            includeRecentApps = true
        )
        return result.toSnapshot(name = name, description = description)
    }
}
