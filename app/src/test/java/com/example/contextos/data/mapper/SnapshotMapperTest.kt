package com.example.contextos.data.mapper

import com.example.contextos.data.local.dao.SnapshotWithItems
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotMapperTest {

    @Test
    fun itemEntityToDomainAndBack_preservesMetadata() {
        val domainItem = ContextItem.createApp(
            snapshotId = "snap-123",
            packageName = "com.android.chrome",
            appName = "Google Chrome",
            launchIntentUri = "https://kotlinlang.org",
            isPrimaryForSplitScreen = true,
            displayOrder = 0,
            id = "item-1"
        )

        val entity = SnapshotMapper.toEntity(domainItem)
        assertEquals("item-1", entity.id)
        assertEquals("snap-123", entity.snapshotId)
        assertEquals(ContextItemType.APP.name, entity.type)
        assertEquals("com.android.chrome", entity.packageName)
        assertEquals("Google Chrome", entity.appName)
        assertNotNull(entity.metadataJson)

        val restoredDomain = SnapshotMapper.toDomain(entity)
        assertEquals(domainItem.id, restoredDomain.id)
        assertEquals(domainItem.type, restoredDomain.type)
        assertEquals(domainItem.packageName, restoredDomain.packageName)
        assertEquals(domainItem.appName, restoredDomain.appName)
        assertEquals("https://kotlinlang.org", restoredDomain.metadata["launchIntentUri"])
        assertTrue(restoredDomain.isPrimaryForSplitScreen)
    }

    @Test
    fun documentItemMapping_retainsFileMetadata() {
        val doc = ContextItem.createDocument(
            snapshotId = "snap-doc",
            contentUri = "content://media/external/doc/1",
            fileUri = "file:///storage/emulated/0/report.pdf",
            displayName = "Quarterly_Report.pdf",
            mimeType = "application/pdf",
            fileSizeBytes = 512000L,
            displayOrder = 2
        )

        val entity = SnapshotMapper.toEntity(doc)
        val mapped = SnapshotMapper.toDomain(entity)

        assertEquals("Quarterly_Report.pdf", mapped.displayName)
        assertEquals("application/pdf", mapped.mimeType)
        assertEquals(512000L, mapped.fileSizeBytes)
        assertEquals("content://media/external/doc/1", mapped.contentUri)
        assertEquals("file:///storage/emulated/0/report.pdf", mapped.fileUri)
    }

    @Test
    fun snapshotWithItemsToDomain_correctlyMapsSnapshotAndItems() {
        val snapshotEntity = SnapshotEntity(
            id = "snap-full",
            name = "Test Snapshot",
            description = "Description here",
            aiSummary = "Executive brief",
            createdAt = 1000L,
            updatedAt = 2000L,
            lastResumedAt = 1500L,
            isPinned = true,
            colorHex = "#2196F3",
            nextActions = listOf("Follow up with team")
        )

        val itemEntities = listOf(
            ContextItemEntity(
                id = "i1",
                snapshotId = "snap-full",
                type = ContextItemType.NOTE.name,
                noteContent = "Remember to push changes",
                displayOrder = 0,
                createdAt = 1000L
            ),
            ContextItemEntity(
                id = "i2",
                snapshotId = "snap-full",
                type = ContextItemType.CLIPBOARD.name,
                noteContent = "export API_KEY=xyz",
                displayOrder = 1,
                createdAt = 1001L
            )
        )

        val relation = SnapshotWithItems(snapshot = snapshotEntity, items = itemEntities)
        val domain = SnapshotMapper.toDomain(relation)

        assertEquals("snap-full", domain.id)
        assertEquals("Test Snapshot", domain.name)
        assertEquals("Executive brief", domain.aiSummary)
        assertEquals(1500L, domain.lastResumedAt)
        assertEquals(1500L, domain.lastRestoredAt)
        assertTrue(domain.isPinned)
        assertEquals(2, domain.items.size)
        assertEquals(1, domain.notes.size)
        assertEquals(1, domain.clipboards.size)
        assertEquals("Remember to push changes", domain.notes.first().noteContent)
        assertEquals("export API_KEY=xyz", domain.clipboards.first().noteContent)
    }

    @Test
    fun domainToSnapshotEntity_correctlyMapsFields() {
        val snapshot = ContextSnapshot(
            id = "snap-to-entity",
            name = "Sprint Context",
            description = "Sprint tasks",
            aiSummary = "Summary text",
            createdAt = 500L,
            updatedAt = 800L,
            lastResumedAt = 600L,
            isPinned = false,
            colorHex = "#009688",
            nextActions = listOf("Task 1")
        )

        val entity = SnapshotMapper.toEntity(snapshot)
        assertEquals(snapshot.id, entity.id)
        assertEquals(snapshot.name, entity.name)
        assertEquals(snapshot.description, entity.description)
        assertEquals(snapshot.aiSummary, entity.aiSummary)
        assertEquals(snapshot.createdAt, entity.createdAt)
        assertEquals(snapshot.updatedAt, entity.updatedAt)
        assertEquals(snapshot.lastResumedAt, entity.lastResumedAt)
        assertEquals(snapshot.isPinned, entity.isPinned)
        assertEquals(snapshot.colorHex, entity.colorHex)
        assertEquals(snapshot.nextActions, entity.nextActions)
    }
}
