package com.example.contextos.voice

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandProcessorTest {

    private lateinit var processor: DeterministicVoiceCommandProcessor

    @Before
    fun setUp() {
        processor = DeterministicVoiceCommandProcessor()
    }

    // --- Primary & Supported RESUME_CONTEXT Commands ---

    @Test
    fun processCommand_resumeProjectAlpha_identifiesResumeIntentAndContextName() = runTest {
        val result = processor.processCommand("Resume Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun processCommand_openProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Open Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_switchToProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Switch to Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_resumeMyProjectAlphaContext_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Resume my Project Alpha context")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_openMyProjectAlphaWorkspace_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Open my Project Alpha workspace")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_switchBackToProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Switch back to Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_restoreProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Restore Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_loadProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Load Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_resumeContextProjectAlpha_identifiesResumeIntent() = runTest {
        val result = processor.processCommand("Resume context Project Alpha")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_punctuationHandledGracefully() = runTest {
        val result = processor.processCommand("Resume Project Alpha.")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
    }

    @Test
    fun processCommand_bareResume_flagsConfirmationRequired() = runTest {
        val result = processor.processCommand("Resume")
        assertEquals(VoiceIntent.RESUME_CONTEXT, result.intent)
        assertNull(result.contextName)
        assertTrue(result.requiresConfirmation)
    }

    // --- SAVE_CONTEXT Commands ---

    @Test
    fun processCommand_saveCurrentContext_identifiesSaveIntent() = runTest {
        val result = processor.processCommand("Save Current Context")
        assertEquals(VoiceIntent.SAVE_CONTEXT, result.intent)
        assertNull(result.contextName)
    }

    @Test
    fun processCommand_saveContext_identifiesSaveIntent() = runTest {
        val result = processor.processCommand("Save context")
        assertEquals(VoiceIntent.SAVE_CONTEXT, result.intent)
        assertNull(result.contextName)
    }

    @Test
    fun processCommand_saveContextAsProjectBeta_identifiesSaveIntentWithContextName() = runTest {
        val result = processor.processCommand("Save context as Project Beta")
        assertEquals(VoiceIntent.SAVE_CONTEXT, result.intent)
        assertEquals("Project Beta", result.contextName)
    }

    @Test
    fun processCommand_saveCurrentContextAsProjectGamma_identifiesSaveIntent() = runTest {
        val result = processor.processCommand("Save current context as Project Gamma")
        assertEquals(VoiceIntent.SAVE_CONTEXT, result.intent)
        assertEquals("Project Gamma", result.contextName)
    }

    @Test
    fun processCommand_captureCurrentContext_identifiesSaveIntent() = runTest {
        val result = processor.processCommand("Capture current context")
        assertEquals(VoiceIntent.SAVE_CONTEXT, result.intent)
    }

    // --- LIST_CONTEXTS Commands ---

    @Test
    fun processCommand_listContexts_identifiesListIntent() = runTest {
        val result = processor.processCommand("List contexts")
        assertEquals(VoiceIntent.LIST_CONTEXTS, result.intent)
    }

    @Test
    fun processCommand_showMyContexts_identifiesListIntent() = runTest {
        val result = processor.processCommand("Show my contexts")
        assertEquals(VoiceIntent.LIST_CONTEXTS, result.intent)
    }

    @Test
    fun processCommand_whatAreMyContexts_identifiesListIntent() = runTest {
        val result = processor.processCommand("What are my contexts")
        assertEquals(VoiceIntent.LIST_CONTEXTS, result.intent)
    }

    @Test
    fun processCommand_showAllWorkspaces_identifiesListIntent() = runTest {
        val result = processor.processCommand("Show all workspaces")
        assertEquals(VoiceIntent.LIST_CONTEXTS, result.intent)
    }

    // --- DELETE_CONTEXT (Destructive Command) ---

    @Test
    fun processCommand_deleteProjectAlpha_identifiesDeleteIntentWithConfirmation() = runTest {
        val result = processor.processCommand("Delete Project Alpha")
        assertEquals(VoiceIntent.DELETE_CONTEXT, result.intent)
        assertEquals("Project Alpha", result.contextName)
        assertTrue(result.requiresConfirmation)
    }

    // --- Unknown & Edge Cases ---

    @Test
    fun processCommand_unknownInput_returnsUnknownIntent() = runTest {
        val result = processor.processCommand("Book flight to Tokyo")
        assertEquals(VoiceIntent.UNKNOWN, result.intent)
        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun processCommand_emptyString_returnsUnknownIntent() = runTest {
        val result = processor.processCommand("   ")
        assertEquals(VoiceIntent.UNKNOWN, result.intent)
    }
}
