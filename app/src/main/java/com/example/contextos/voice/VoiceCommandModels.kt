package com.example.contextos.voice

import com.example.contextos.models.ContextSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recognized voice intents for ContextOS.
 */
enum class VoiceIntent {
    RESUME_CONTEXT,
    SAVE_CONTEXT,
    LIST_CONTEXTS,
    DELETE_CONTEXT,
    UNKNOWN
}

/**
 * Structured parsed output from a voice command transcript.
 *
 * @param intent The identified [VoiceIntent].
 * @param contextName Extracted context name, if specified (e.g. "Project Alpha").
 * @param rawSpokenText The raw transcript from speech recognition.
 * @param confidence Confidence score (0.0 to 1.0).
 * @param requiresConfirmation True if ambiguous or destructive command requires explicit confirmation.
 * @param note Optional parsing rationale or diagnostic message.
 */
data class ParsedVoiceCommand(
    val intent: VoiceIntent,
    val contextName: String? = null,
    val rawSpokenText: String,
    val confidence: Float = 1.0f,
    val requiresConfirmation: Boolean = false,
    val note: String? = null
)

/**
 * Matching result when searching for a context snapshot by name.
 */
sealed interface ContextMatchResult {
    data class ExactMatch(val snapshot: ContextSnapshot) : ContextMatchResult
    data class SingleMatch(val snapshot: ContextSnapshot) : ContextMatchResult
    data class AmbiguousMatch(val query: String, val candidates: List<ContextSnapshot>) : ContextMatchResult
    data class NoMatch(val query: String, val availableSnapshots: List<ContextSnapshot>) : ContextMatchResult
    data class NoneSpecified(val availableSnapshots: List<ContextSnapshot>) : ContextMatchResult
}

/**
 * Interface for processing voice input transcripts into structured [ParsedVoiceCommand] instances.
 *
 * Pluggable architecture:
 * - [DeterministicVoiceCommandProcessor]: Fast, deterministic regex/rule-based parser.
 * - Future on-device AI models (e.g., Gemini Nano / AICore) can implement this interface and be swapped
 *   seamlessly via Dependency Injection without modifying downstream restore engines or presentation layers.
 */
interface VoiceCommandProcessor {
    suspend fun processCommand(spokenText: String): ParsedVoiceCommand
}

/**
 * Deterministic implementation of [VoiceCommandProcessor].
 * Supports natural phrasing variations including:
 * - "Resume Project Alpha"
 * - "Open Project Alpha"
 * - "Switch to Project Alpha"
 * - "Resume my Project Alpha context"
 * - "Restore Project Alpha"
 * - "Save current context" / "Save context as Project Beta"
 * - "List contexts" / "Show my contexts"
 */
@Singleton
class DeterministicVoiceCommandProcessor @Inject constructor() : VoiceCommandProcessor {

    override suspend fun processCommand(spokenText: String): ParsedVoiceCommand {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) {
            return ParsedVoiceCommand(
                intent = VoiceIntent.UNKNOWN,
                rawSpokenText = spokenText,
                confidence = 0f,
                requiresConfirmation = false,
                note = "Spoken text is empty"
            )
        }

        // Clean trailing punctuation
        val cleaned = trimmed.replace(Regex("""[.?!,]+$"""), "").trim()

        // 1. LIST_CONTEXTS patterns
        // e.g. "list contexts", "list my contexts", "show contexts", "show all contexts", "what are my contexts", "view contexts"
        val listRegex = Regex(
            """^(?:list|show|view|get|display|what\s+are)\s+(?:all\s+|my\s+)?(?:contexts|workspaces|snapshots)$""",
            RegexOption.IGNORE_CASE
        )
        if (listRegex.matches(cleaned)) {
            return ParsedVoiceCommand(
                intent = VoiceIntent.LIST_CONTEXTS,
                rawSpokenText = spokenText,
                confidence = 1.0f
            )
        }

        // 2. SAVE_CONTEXT patterns
        // e.g. "save current context as Project Alpha", "save context as Project Alpha", "save context Project Alpha"
        val saveWithNamedRegex = Regex(
            """^(?:save|capture)\s+(?:current\s+)?(?:context|workspace)\s+(?:as\s+|named\s+)?(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val saveNamedMatch = saveWithNamedRegex.find(cleaned)
        if (saveNamedMatch != null) {
            val rawName = cleanContextName(saveNamedMatch.groupValues[1])
            return ParsedVoiceCommand(
                intent = VoiceIntent.SAVE_CONTEXT,
                contextName = rawName.ifBlank { null },
                rawSpokenText = spokenText,
                confidence = 0.95f
            )
        }

        // e.g. "save current context", "save context", "capture current context", "save workspace", "save"
        val saveSimpleRegex = Regex(
            """^(?:save|capture)\s+(?:current\s+)?(?:context|workspace|state)$""",
            RegexOption.IGNORE_CASE
        )
        if (saveSimpleRegex.matches(cleaned) || cleaned.equals("save", ignoreCase = true) || cleaned.equals("save context", ignoreCase = true)) {
            return ParsedVoiceCommand(
                intent = VoiceIntent.SAVE_CONTEXT,
                contextName = null,
                rawSpokenText = spokenText,
                confidence = 0.95f
            )
        }

        // 3. DELETE_CONTEXT patterns (destructive command, flag confirmation)
        // e.g. "delete Project Alpha", "delete context Project Alpha", "remove Project Alpha"
        val deleteRegex = Regex(
            """^(?:delete|remove)\s+(?:the\s+)?(?:context|workspace\s+)?(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val deleteMatch = deleteRegex.find(cleaned)
        if (deleteMatch != null) {
            val rawName = cleanContextName(deleteMatch.groupValues[1])
            if (rawName.isNotBlank() && !rawName.equals("context", ignoreCase = true) && !rawName.equals("workspace", ignoreCase = true)) {
                return ParsedVoiceCommand(
                    intent = VoiceIntent.DELETE_CONTEXT,
                    contextName = rawName,
                    rawSpokenText = spokenText,
                    confidence = 0.95f,
                    requiresConfirmation = true,
                    note = "Destructive command requires explicit confirmation"
                )
            }
        }

        // 4. RESUME_CONTEXT patterns
        // Matches:
        // - "Resume my Project Alpha context"
        // - "Open my Project Alpha context"
        // - "Switch to my Project Alpha context"
        // - "Switch back to Project Alpha"
        // - "Resume Project Alpha"
        // - "Open Project Alpha"
        // - "Switch to Project Alpha"
        // - "Restore Project Alpha"
        // - "Load Project Alpha"
        // - "Resume context Project Alpha"
        val resumePatterns = listOf(
            // "resume/open/restore/load/switch to/switch back to my <name> context/workspace"
            Regex(
                """^(?:resume|open|restore|load|switch\s+to|switch\s+back\s+to)\s+my\s+(.+?)(?:\s+context|\s+workspace)?$""",
                RegexOption.IGNORE_CASE
            ),
            // "resume/open/restore/load/switch to/switch back to the/context/workspace <name>"
            Regex(
                """^(?:resume|open|restore|load|switch\s+to|switch\s+back\s+to)\s+(?:the\s+)?(?:context|workspace)\s+(.+)$""",
                RegexOption.IGNORE_CASE
            ),
            // "resume/open/restore/load/switch to/switch back to <name> context/workspace"
            Regex(
                """^(?:resume|open|restore|load|switch\s+to|switch\s+back\s+to)\s+(.+?)(?:\s+context|\s+workspace)?$""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in resumePatterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val extracted = cleanContextName(match.groupValues[1])
                if (extracted.isNotBlank()) {
                    return ParsedVoiceCommand(
                        intent = VoiceIntent.RESUME_CONTEXT,
                        contextName = extracted,
                        rawSpokenText = spokenText,
                        confidence = 0.98f
                    )
                }
            }
        }

        // Bare "resume", "restore", "switch" without target context
        val bareResumeRegex = Regex("""^(?:resume|restore|switch)$""", RegexOption.IGNORE_CASE)
        if (bareResumeRegex.matches(cleaned)) {
            return ParsedVoiceCommand(
                intent = VoiceIntent.RESUME_CONTEXT,
                contextName = null,
                rawSpokenText = spokenText,
                confidence = 0.70f,
                requiresConfirmation = true,
                note = "Context name unspecified"
            )
        }

        return ParsedVoiceCommand(
            intent = VoiceIntent.UNKNOWN,
            rawSpokenText = spokenText,
            confidence = 0f,
            requiresConfirmation = false,
            note = "Could not identify command intent"
        )
    }

    private fun cleanContextName(raw: String): String {
        return raw.trim()
            .removeSurrounding("\"", "\"")
            .removeSurrounding("'", "'")
            .replace(Regex("""^(?:the|my)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:context|workspace)$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

/**
 * Matches a query against existing [ContextSnapshot]s with multi-tier resolution:
 * 1. Exact case-insensitive match
 * 2. Prefix/substring match
 * 3. Word token matching
 * 4. Ambiguity resolution when multiple snapshots match
 */
@Singleton
class ContextMatcher @Inject constructor() {

    fun match(query: String?, snapshots: List<ContextSnapshot>): ContextMatchResult {
        val trimmed = query?.trim()
        if (trimmed.isNullOrBlank()) {
            return ContextMatchResult.NoneSpecified(snapshots)
        }

        val lowerQuery = trimmed.lowercase()

        // 1. Exact match (case-insensitive)
        val exact = snapshots.firstOrNull { it.name.trim().lowercase() == lowerQuery }
        if (exact != null) {
            return ContextMatchResult.ExactMatch(exact)
        }

        // 2. Substring or prefix matches
        val substringCandidates = snapshots.filter { snapshot ->
            val snapshotName = snapshot.name.trim().lowercase()
            snapshotName.contains(lowerQuery) || lowerQuery.contains(snapshotName)
        }

        if (substringCandidates.size == 1) {
            return ContextMatchResult.SingleMatch(substringCandidates.first())
        }
        if (substringCandidates.size > 1) {
            return ContextMatchResult.AmbiguousMatch(trimmed, substringCandidates)
        }

        // 3. Word-level token match (e.g. "Alpha" matches "Project Alpha")
        val queryTokens = lowerQuery.split(Regex("""\s+""")).filter { it.length > 1 }
        val tokenCandidates = snapshots.filter { snapshot ->
            val snapshotTokens = snapshot.name.lowercase().split(Regex("""\s+""")).filter { it.length > 1 }
            queryTokens.any { qt -> snapshotTokens.any { st -> st.contains(qt) || qt.contains(st) } }
        }

        return when {
            tokenCandidates.size == 1 -> ContextMatchResult.SingleMatch(tokenCandidates.first())
            tokenCandidates.size > 1 -> ContextMatchResult.AmbiguousMatch(trimmed, tokenCandidates)
            else -> ContextMatchResult.NoMatch(trimmed, snapshots)
        }
    }
}
