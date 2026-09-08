package com.example.contextos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.data.local.dao.ContextItemDao
import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.local.entity.ContextItemEntity
import com.example.contextos.data.local.entity.SnapshotEntity
import com.example.contextos.data.sample.SampleData
import com.example.contextos.models.ContextItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ContextOsDatabaseTest {

    private lateinit var database: ContextOsDatabase
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var contextItemDao: ContextItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ContextOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        snapshotDao = database.snapshotDao()
        contextItemDao = database.contextItemDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndReadSnapshot() = runTest {
        val snapshot = SnapshotEntity(
            id = "test-snap-1",
            name = "Project Testing",
            description = "Test description",
            aiSummary = "Summary of testing context",
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null,
            isPinned = false,
            colorHex = "#FF5722",
            nextActions = listOf("Action 1", "Action 2")
        )

        snapshotDao.insertSnapshot(snapshot)
        val loaded = snapshotDao.getSnapshotEntityById("test-snap-1")

        assertNotNull(loaded)
        assertEquals("test-snap-1", loaded?.id)
        assertEquals("Project Testing", loaded?.name)
        assertEquals("Test description", loaded?.description)
        assertEquals("Summary of testing context", loaded?.aiSummary)
        assertEquals(2, loaded?.nextActions?.size)
        assertEquals("Action 1", loaded?.nextActions?.get(0))
    }

    @Test
    fun updateSnapshot() = runTest {
        val snapshot = SnapshotEntity(
            id = "test-snap-update",
            name = "Initial Name",
            description = "Initial Description",
            aiSummary = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null,
            isPinned = false,
            colorHex = "#1E88E5"
        )
        snapshotDao.insertSnapshot(snapshot)

        val updated = snapshot.copy(
            name = "Updated Name",
            description = "Updated Description",
            aiSummary = "Generated brief",
            updatedAt = 2000L
        )
        snapshotDao.updateSnapshot(updated)

        val loaded = snapshotDao.getSnapshotEntityById("test-snap-update")
        assertNotNull(loaded)
        assertEquals("Updated Name", loaded?.name)
        assertEquals("Updated Description", loaded?.description)
        assertEquals("Generated brief", loaded?.aiSummary)
        assertEquals(2000L, loaded?.updatedAt)
    }

    @Test
    fun deleteSnapshot() = runTest {
        val snapshot = SnapshotEntity(
            id = "test-snap-delete",
            name = "To be deleted",
            description = "",
            aiSummary = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null
        )
        snapshotDao.insertSnapshot(snapshot)
        assertNotNull(snapshotDao.getSnapshotEntityById("test-snap-delete"))

        val deletedCount = snapshotDao.deleteSnapshotById("test-snap-delete")
        assertEquals(1, deletedCount)
        assertNull(snapshotDao.getSnapshotEntityById("test-snap-delete"))
    }

    @Test
    fun updatePinnedAndLastResumed() = runTest {
        val snapshot = SnapshotEntity(
            id = "test-snap-pin",
            name = "Pin Test",
            description = "",
            aiSummary = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null,
            isPinned = false
        )
        snapshotDao.insertSnapshot(snapshot)

        snapshotDao.updatePinned("test-snap-pin", true, 1500L)
        var loaded = snapshotDao.getSnapshotEntityById("test-snap-pin")
        assertTrue(loaded?.isPinned == true)
        assertEquals(1500L, loaded?.updatedAt)

        snapshotDao.updateLastResumed("test-snap-pin", 2500L, 2500L)
        loaded = snapshotDao.getSnapshotEntityById("test-snap-pin")
        assertEquals(2500L, loaded?.lastResumedAt)
        assertEquals(2500L, loaded?.updatedAt)
    }

    @Test
    fun contextItemDao_crudOperations() = runTest {
        val snapshot = SnapshotEntity(
            id = "snap-items-test",
            name = "Snapshot with Items",
            description = "",
            aiSummary = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null
        )
        snapshotDao.insertSnapshot(snapshot)

        val item1 = ContextItemEntity(
            id = "item-app-1",
            snapshotId = "snap-items-test",
            type = ContextItemType.APP.name,
            packageName = "com.google.android.apps.docs",
            appName = "Drive",
            displayOrder = 0,
            createdAt = 1000L
        )
        val item2 = ContextItemEntity(
            id = "item-url-2",
            snapshotId = "snap-items-test",
            type = ContextItemType.URL.name,
            url = "https://example.com",
            displayOrder = 1,
            createdAt = 1001L
        )

        contextItemDao.insertItem(item1)
        contextItemDao.insertItem(item2)

        val allItems = contextItemDao.getItemsForSnapshot("snap-items-test").first()
        assertEquals(2, allItems.size)
        assertEquals("Drive", allItems[0].appName)
        assertEquals("https://example.com", allItems[1].url)

        val appItemsOnly = contextItemDao.getItemsByType("snap-items-test", ContextItemType.APP.name).first()
        assertEquals(1, appItemsOnly.size)
        assertEquals("Drive", appItemsOnly[0].appName)

        // Update item
        contextItemDao.updateItem(item1.copy(appName = "Google Drive Updated"))
        val loadedItem = contextItemDao.getItemById("item-app-1")
        assertEquals("Google Drive Updated", loadedItem?.appName)

        // Delete item
        val deleteCount = contextItemDao.deleteItemById("item-url-2")
        assertEquals(1, deleteCount)
        val remainingItems = contextItemDao.getItemsForSnapshot("snap-items-test").first()
        assertEquals(1, remainingItems.size)
    }

    @Test
    fun foreignKeyCascade_deletingSnapshotDeletesContextItems() = runTest {
        val snapshot = SnapshotEntity(
            id = "snap-cascade",
            name = "Cascade Test",
            description = "",
            aiSummary = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null
        )
        snapshotDao.insertSnapshot(snapshot)

        val item1 = ContextItemEntity(
            id = "item-casc-1",
            snapshotId = "snap-cascade",
            type = ContextItemType.NOTE.name,
            noteContent = "Temporary sticky note",
            createdAt = 1000L
        )
        val item2 = ContextItemEntity(
            id = "item-casc-2",
            snapshotId = "snap-cascade",
            type = ContextItemType.CLIPBOARD.name,
            noteContent = "Copied text snippet",
            createdAt = 1001L
        )
        contextItemDao.insertItems(listOf(item1, item2))

        val itemsBefore = contextItemDao.getItemsForSnapshot("snap-cascade").first()
        assertEquals(2, itemsBefore.size)

        // Delete snapshot -> cascading delete removes its items
        snapshotDao.deleteSnapshotById("snap-cascade")

        val itemsAfter = contextItemDao.getItemsForSnapshot("snap-cascade").first()
        assertTrue(itemsAfter.isEmpty())
        assertNull(contextItemDao.getItemById("item-casc-1"))
        assertNull(contextItemDao.getItemById("item-casc-2"))
    }

    @Test
    fun upsertFullSnapshotWithRelationQuery() = runTest {
        val snapshot = SnapshotEntity(
            id = "snap-full-1",
            name = "Full Workspace",
            description = "Test description",
            aiSummary = "Smart executive summary",
            createdAt = 1000L,
            updatedAt = 1000L,
            lastResumedAt = null,
            isPinned = true
        )

        val items = listOf(
            ContextItemEntity(
                id = "item-f-1",
                snapshotId = "snap-full-1",
                type = ContextItemType.APP.name,
                packageName = "com.android.chrome",
                appName = "Chrome",
                displayOrder = 0,
                createdAt = 1000L
            ),
            ContextItemEntity(
                id = "item-f-2",
                snapshotId = "snap-full-1",
                type = ContextItemType.DOCUMENT.name,
                contentUri = "content://docs/101",
                fileUri = "file:///storage/docs/doc1.pdf",
                appName = "Spec.pdf",
                displayOrder = 1,
                createdAt = 1001L
            ),
            ContextItemEntity(
                id = "item-f-3",
                snapshotId = "snap-full-1",
                type = ContextItemType.IMAGE.name,
                imageUri = "file:///images/whiteboard.jpg",
                noteContent = "OCR: Plan v1",
                displayOrder = 2,
                createdAt = 1002L
            )
        )

        snapshotDao.upsertFullSnapshot(snapshot, items)

        val withItems = snapshotDao.getSnapshotWithItemsById("snap-full-1").first()
        assertNotNull(withItems)
        assertEquals("Full Workspace", withItems?.snapshot?.name)
        assertEquals(3, withItems?.items?.size)

        // Upsert again with new set of items (replaces existing items atomically)
        val updatedItems = listOf(
            ContextItemEntity(
                id = "item-f-new",
                snapshotId = "snap-full-1",
                type = ContextItemType.NOTE.name,
                noteContent = "Only this note remains",
                displayOrder = 0,
                createdAt = 2000L
            )
        )
        snapshotDao.upsertFullSnapshot(snapshot.copy(name = "Updated Workspace"), updatedItems)

        val reloaded = snapshotDao.getSnapshotWithItemsById("snap-full-1").first()
        assertNotNull(reloaded)
        assertEquals("Updated Workspace", reloaded?.snapshot?.name)
        assertEquals(1, reloaded?.items?.size)
        assertEquals("item-f-new", reloaded?.items?.first()?.id)
        assertEquals(ContextItemType.NOTE.name, reloaded?.items?.first()?.type)
    }

    @Test
    fun sampleData_projectAlphaAndBetaVerification() = runTest {
        val alpha = SampleData.getProjectAlpha()
        val beta = SampleData.getProjectBeta()

        assertEquals("Project Alpha", alpha.name)
        assertEquals("Project Beta", beta.name)

        // Verify all 6 ContextItem types are represented
        val allTypes = listOf(
            ContextItemType.APP,
            ContextItemType.URL,
            ContextItemType.DOCUMENT,
            ContextItemType.NOTE,
            ContextItemType.IMAGE,
            ContextItemType.CLIPBOARD
        )

        val alphaTypes = alpha.items.map { it.type }.toSet()
        val betaTypes = beta.items.map { it.type }.toSet()

        assertTrue(alphaTypes.containsAll(allTypes))
        assertTrue(betaTypes.containsAll(allTypes))

        // Verify Project Alpha has AI summary and next actions
        assertNotNull(alpha.aiSummary)
        assertTrue(alpha.aiSummary!!.contains("Project Alpha"))
        assertEquals(3, alpha.nextActions.size)

        // Verify Project Beta has AI summary and next actions
        assertNotNull(beta.aiSummary)
        assertTrue(beta.aiSummary!!.contains("Project Beta"))
        assertEquals(3, beta.nextActions.size)
    }
}
