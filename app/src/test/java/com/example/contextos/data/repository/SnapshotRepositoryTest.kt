package com.example.contextos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.contextos.data.local.ContextOsDatabase
import com.example.contextos.data.local.dao.ContextItemDao
import com.example.contextos.data.local.dao.SnapshotDao
import com.example.contextos.data.sample.SampleData
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.domain.usecase.DeleteSnapshotUseCase
import com.example.contextos.domain.usecase.GetSnapshotByIdUseCase
import com.example.contextos.domain.usecase.GetSnapshotsUseCase
import com.example.contextos.domain.usecase.MarkSnapshotResumedUseCase
import com.example.contextos.domain.usecase.SaveSnapshotUseCase
import com.example.contextos.domain.usecase.SeedSampleDataUseCase
import com.example.contextos.domain.usecase.TogglePinSnapshotUseCase
import com.example.contextos.models.ContextItem
import com.example.contextos.models.ContextItemType
import com.example.contextos.models.ContextSnapshot
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
class SnapshotRepositoryTest {

    private lateinit var database: ContextOsDatabase
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var contextItemDao: ContextItemDao
    private lateinit var repository: SnapshotRepository

    // Use cases under test
    private lateinit var getSnapshotsUseCase: GetSnapshotsUseCase
    private lateinit var getSnapshotByIdUseCase: GetSnapshotByIdUseCase
    private lateinit var saveSnapshotUseCase: SaveSnapshotUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase
    private lateinit var togglePinSnapshotUseCase: TogglePinSnapshotUseCase
    private lateinit var markSnapshotResumedUseCase: MarkSnapshotResumedUseCase
    private lateinit var seedSampleDataUseCase: SeedSampleDataUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ContextOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        snapshotDao = database.snapshotDao()
        contextItemDao = database.contextItemDao()
        repository = SnapshotRepositoryImpl(snapshotDao, contextItemDao)

        getSnapshotsUseCase = GetSnapshotsUseCase(repository)
        getSnapshotByIdUseCase = GetSnapshotByIdUseCase(repository)
        saveSnapshotUseCase = SaveSnapshotUseCase(repository)
        deleteSnapshotUseCase = DeleteSnapshotUseCase(repository)
        togglePinSnapshotUseCase = TogglePinSnapshotUseCase(repository)
        markSnapshotResumedUseCase = MarkSnapshotResumedUseCase(repository)
        seedSampleDataUseCase = SeedSampleDataUseCase(repository)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun repository_saveAndGetSnapshot() = runTest {
        val alpha = SampleData.getProjectAlpha()
        repository.saveSnapshot(alpha)

        val list = repository.getSnapshots().first()
        assertEquals(1, list.size)
        assertEquals(alpha.name, list.first().name)
        assertEquals(alpha.items.size, list.first().items.size)

        val retrieved = repository.getSnapshotById(alpha.id).first()
        assertNotNull(retrieved)
        assertEquals(alpha.id, retrieved?.id)
        assertEquals(alpha.name, retrieved?.name)
        assertEquals(alpha.aiSummary, retrieved?.aiSummary)
        assertEquals(alpha.items.size, retrieved?.items?.size)
    }

    @Test
    fun repository_deleteSnapshot() = runTest {
        val beta = SampleData.getProjectBeta()
        repository.saveSnapshot(beta)

        var list = repository.getSnapshots().first()
        assertEquals(1, list.size)

        repository.deleteSnapshot(beta.id)

        list = repository.getSnapshots().first()
        assertTrue(list.isEmpty())

        val items = repository.getItemsForSnapshot(beta.id).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun repository_markResumedAndTogglePin() = runTest {
        val snapshot = ContextSnapshot(
            id = "test-resume-pin",
            name = "Interactive Snapshot",
            isPinned = false,
            lastResumedAt = null
        )
        repository.saveSnapshot(snapshot)

        repository.togglePin("test-resume-pin", true)
        var updated = repository.getSnapshotById("test-resume-pin").first()
        assertTrue(updated?.isPinned == true)

        val now = 50000L
        repository.markResumed("test-resume-pin", now)
        updated = repository.getSnapshotById("test-resume-pin").first()
        assertEquals(now, updated?.lastResumedAt)
        assertEquals(now, updated?.lastRestoredAt)
    }

    @Test
    fun repository_seedInitialData_addsAlphaAndBeta() = runTest {
        val before = repository.getSnapshots().first()
        assertTrue(before.isEmpty())

        repository.seedInitialData()

        val after = repository.getSnapshots().first()
        assertEquals(2, after.size)

        val names = after.map { it.name }.toSet()
        assertTrue(names.contains("Project Alpha"))
        assertTrue(names.contains("Project Beta"))

        // Calling seedInitialData again should be idempotent (does not duplicate)
        repository.seedInitialData()
        val afterSecondSeed = repository.getSnapshots().first()
        assertEquals(2, afterSecondSeed.size)
    }

    @Test
    fun repository_individualContextItemOperations() = runTest {
        val snapshot = ContextSnapshot(
            id = "snap-single-items",
            name = "Snapshot for Single Items"
        )
        repository.saveSnapshot(snapshot)

        val item1 = ContextItem.createApp(
            snapshotId = "snap-single-items",
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            id = "item-yt"
        )
        val item2 = ContextItem.createNote(
            snapshotId = "snap-single-items",
            content = "Quick thought",
            id = "item-note"
        )

        repository.saveContextItem(item1)
        repository.saveContextItem(item2)

        val items = repository.getItemsForSnapshot("snap-single-items").first()
        assertEquals(2, items.size)

        repository.deleteContextItem("item-yt")
        val remaining = repository.getItemsForSnapshot("snap-single-items").first()
        assertEquals(1, remaining.size)
        assertEquals("item-note", remaining.first().id)
    }

    @Test
    fun useCases_integrationTest() = runTest {
        // Seed sample data use case
        seedSampleDataUseCase()

        // Get snapshots use case
        val snapshots = getSnapshotsUseCase().first()
        assertEquals(2, snapshots.size)

        // Get by ID use case
        val alpha = getSnapshotByIdUseCase(SampleData.PROJECT_ALPHA_ID).first()
        assertNotNull(alpha)
        assertEquals("Project Alpha", alpha?.name)

        // Toggle pin use case
        togglePinSnapshotUseCase(SampleData.PROJECT_ALPHA_ID, false)
        val alphaUnpinned = getSnapshotByIdUseCase(SampleData.PROJECT_ALPHA_ID).first()
        assertFalse(alphaUnpinned?.isPinned == true)

        // Mark resumed use case
        markSnapshotResumedUseCase(SampleData.PROJECT_ALPHA_ID, 99999L)
        val alphaResumed = getSnapshotByIdUseCase(SampleData.PROJECT_ALPHA_ID).first()
        assertEquals(99999L, alphaResumed?.lastResumedAt)

        // Delete use case
        deleteSnapshotUseCase(SampleData.PROJECT_ALPHA_ID)
        val remaining = getSnapshotsUseCase().first()
        assertEquals(1, remaining.size)
        assertEquals("Project Beta", remaining.first().name)
    }
}
