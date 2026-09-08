package com.example.contextos.data.sample

import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextSnapshot

/**
 * Pre-configured sample contexts for testing, onboarding, and demonstrations.
 */
object SampleData {

    const val PROJECT_ALPHA_ID = "snapshot-project-alpha-001"
    const val PROJECT_BETA_ID = "snapshot-project-beta-002"

    fun getProjectAlpha(snapshotId: String = PROJECT_ALPHA_ID): ContextSnapshot {
        val now = System.currentTimeMillis()
        val items = listOf(
            ContextItem.createApp(
                snapshotId = snapshotId,
                packageName = "com.google.android.apps.docs",
                appName = "Google Drive",
                launchIntentUri = "content://com.google.android.apps.docs/root",
                isPrimaryForSplitScreen = true,
                displayOrder = 0
            ),
            ContextItem.createApp(
                snapshotId = snapshotId,
                packageName = "com.android.chrome",
                appName = "Google Chrome",
                launchIntentUri = "https://github.com/project-alpha/roadmap",
                isPrimaryForSplitScreen = true,
                displayOrder = 1
            ),
            ContextItem.createUrl(
                snapshotId = snapshotId,
                url = "https://github.com/project-alpha/roadmap",
                title = "Sprint Roadmap & Milestones",
                displayOrder = 2
            ),
            ContextItem.createUrl(
                snapshotId = snapshotId,
                url = "https://console.cloud.google.com/billing",
                title = "GCP Cloud Cost Overview",
                displayOrder = 3
            ),
            ContextItem.createDocument(
                snapshotId = snapshotId,
                contentUri = "content://com.android.providers.downloads.documents/document/104",
                fileUri = "file:///storage/emulated/0/Download/Alpha_Architecture_v2.pdf",
                displayName = "Alpha_Architecture_v2.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 4_200_000L,
                displayOrder = 4
            ),
            ContextItem.createNote(
                snapshotId = snapshotId,
                content = "Finalize multi-region database replication SLA before client sync.",
                isCompleted = false,
                displayOrder = 5
            ),
            ContextItem.createNote(
                snapshotId = snapshotId,
                content = "Review whiteboard sticky notes from 10am standup.",
                isCompleted = false,
                displayOrder = 6
            ),
            ContextItem.createImage(
                snapshotId = snapshotId,
                imageUri = "file:///data/user/0/com.example.contextos/files/standup_board_alpha.jpg",
                ocrRawText = "Q4 Goals: 1. P99 latency < 200ms 2. Zero-downtime migration 3. SOC2 compliance",
                displayOrder = 7
            ),
            ContextItem.createClipboard(
                snapshotId = snapshotId,
                content = "git clone git@github.com:contextos/alpha-service.git && cd alpha-service && ./gradlew check",
                mimeType = "text/plain",
                displayOrder = 8
            )
        )

        return ContextSnapshot(
            id = snapshotId,
            name = "Project Alpha",
            description = "Sprint planning review, architecture diagrams & cloud cost spreadsheets",
            aiSummary = "Context focused on Project Alpha infrastructure scaling, multi-region replication architecture, and cloud billing optimization.",
            createdAt = now - 86400000L * 2, // 2 days ago
            updatedAt = now - 3600000L * 4,
            lastResumedAt = now - 3600000L * 1,
            isPinned = true,
            colorHex = "#1E88E5",
            items = items,
            nextActions = listOf(
                "Review Q4 latency targets in whiteboard notes",
                "Confirm billing alert threshold in Cloud Console",
                "Review PDF section 4 on database replication"
            )
        )
    }

    fun getProjectBeta(snapshotId: String = PROJECT_BETA_ID): ContextSnapshot {
        val now = System.currentTimeMillis()
        val items = listOf(
            ContextItem.createApp(
                snapshotId = snapshotId,
                packageName = "com.figma.android",
                appName = "Figma",
                launchIntentUri = "https://www.figma.com/file/beta-mobile-v2",
                isPrimaryForSplitScreen = true,
                displayOrder = 0
            ),
            ContextItem.createApp(
                snapshotId = snapshotId,
                packageName = "com.Slack",
                appName = "Slack",
                displayOrder = 1
            ),
            ContextItem.createUrl(
                snapshotId = snapshotId,
                url = "https://www.figma.com/file/beta-mobile-v2/ContextOS-UI",
                title = "Beta Mobile Design System & Components",
                displayOrder = 2
            ),
            ContextItem.createUrl(
                snapshotId = snapshotId,
                url = "https://miro.com/app/board/beta-ux-journey",
                title = "Customer Journey Mapping Board",
                displayOrder = 3
            ),
            ContextItem.createDocument(
                snapshotId = snapshotId,
                contentUri = "content://com.android.providers.downloads.documents/document/205",
                fileUri = "file:///storage/emulated/0/Download/Beta_UX_Research_Synthesis.pdf",
                displayName = "Beta_UX_Research_Synthesis.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 2_800_000L,
                displayOrder = 4
            ),
            ContextItem.createNote(
                snapshotId = snapshotId,
                content = "Ensure touch targets for icon buttons meet 48dp minimum accessible size.",
                isCompleted = false,
                displayOrder = 5
            ),
            ContextItem.createNote(
                snapshotId = snapshotId,
                content = "Export SVG vector assets for bottom navigation bar.",
                isCompleted = true,
                displayOrder = 6
            ),
            ContextItem.createImage(
                snapshotId = snapshotId,
                imageUri = "file:///data/user/0/com.example.contextos/files/user_flow_sketch_beta.png",
                ocrRawText = "Screen flow: Onboarding -> Permission Prompt -> Instant Snapshot Dashboard",
                displayOrder = 7
            ),
            ContextItem.createClipboard(
                snapshotId = snapshotId,
                content = "Primary: #6750A4, Secondary: #625B71, Surface: #FEF7FF",
                mimeType = "text/plain",
                displayOrder = 8
            )
        )

        return ContextSnapshot(
            id = snapshotId,
            name = "Project Beta",
            description = "Mobile UX redesign, customer interview synthesis & design system tokens",
            aiSummary = "Design and product synthesis context for Project Beta mobile UX overhaul. Includes Figma wireframe links, customer interview audio notes, and Material 3 design tokens.",
            createdAt = now - 86400000L, // 1 day ago
            updatedAt = now - 1800000L,
            lastResumedAt = null,
            isPinned = false,
            colorHex = "#7C4DFF",
            items = items,
            nextActions = listOf(
                "Audit color contrast on dark mode component tokens",
                "Summarize feedback from User Testing Session #3",
                "Update navigation transitions in Figma prototype"
            )
        )
    }

    fun getAllSampleSnapshots(): List<ContextSnapshot> = listOf(
        getProjectAlpha(),
        getProjectBeta()
    )
}
