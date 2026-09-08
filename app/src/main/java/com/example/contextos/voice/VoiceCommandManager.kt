package com.example.contextos.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.contextos.domain.repository.SnapshotRepository
import com.example.contextos.models.ContextSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State representing the voice recognition and restoration pipeline.
 */
sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Hearing(val partialText: String = ", val rmsdB: Float = 0f) : VoiceState
 data class Processing(val spokenText: String) : VoiceState
 data class MatchFound(
 val command: ParsedVoiceCommand,
 val snapshot: ContextSnapshot
 ) : VoiceState
 data class Ambiguous(
 val command: ParsedVoiceCommand,
 val candidates: List<ContextSnapshot>
 ) : VoiceState
 data class IntentRecognized(
 val command: ParsedVoiceCommand,
 val snapshot: ContextSnapshot? = null
 ) : VoiceState
 data class Unrecognized(
 val spokenText: String,
 val reason: String
 ) : VoiceState
 data class Error(val message: String) : VoiceState
}

/**
 * Contract for managing speech recognition and delegating to the parser and matcher.
 */
interface VoiceCommandManager {
 val listeningState: StateFlow<VoiceState>
 fun startListening(candidateSnapshots: List<ContextSnapshot>? = null)
 fun stopListening()
 fun processText(text: String, candidateSnapshots: List<ContextSnapshot>? = null)
 fun resetState()
}

@Singleton
class VoiceCommandManagerImpl @Inject constructor(
 @ApplicationContext private val context: Context,
 private val voiceCommandProcessor: VoiceCommandProcessor,
 private val contextMatcher: ContextMatcher,
 private val snapshotRepository: SnapshotRepository
) : VoiceCommandManager {

 private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
 private val mainHandler = Handler(Looper.getMainLooper())

 private val _listeningState = MutableStateFlow<VoiceState>(VoiceState.Idle)
 override val listeningState: StateFlow<VoiceState> = _listeningState.asStateFlow()

 private var speechRecognizer: SpeechRecognizer? = null
 private var cachedSnapshots: List<ContextSnapshot>? = null
 private var lastPartialText: String = 

 override fun startListening(candidateSnapshots: List<ContextSnapshot>?) {
 cachedSnapshots = candidateSnapshots
 lastPartialText = 

 // Verify microphone permission
 val hasPermission = ContextCompat.checkSelfPermission(
 context,
 Manifest.permission.RECORD_AUDIO
 ) == PackageManager.PERMISSION_GRANTED

 if (!hasPermission) {
 _listeningState.value = VoiceState.Error(Microphone permission (RECORD_AUDIO) not granted.)
 return
 }

 // Verify device supports speech recognition
 if (!SpeechRecognizer.isRecognitionAvailable(context)) {
 _listeningState.value = VoiceState.Error(
 Speech recognition service is not available on this device. You can type commands manually.
 )
 return
 }

 mainHandler.post {
 try {
 destroyRecognizer()
 val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
 speechRecognizer = recognizer

 recognizer.setRecognitionListener(object : RecognitionListener {
 override fun onReadyForSpeech(params: Bundle?) {
 _listeningState.value = VoiceState.Listening
 }

 override fun onBeginningOfSpeech() {
 _listeningState.value = VoiceState.Hearing(lastPartialText)
 }

 override fun onRmsChanged(rmsdB: Float) {
 if (_listeningState.value is VoiceState.Hearing || _listeningState.value is VoiceState.Listening) {
 _listeningState.value = VoiceState.Hearing(lastPartialText, rmsdB)
 }
 }

 override fun onBufferReceived(buffer: ByteArray?) {}

 override fun onEndOfSpeech() {
 _listeningState.value = VoiceState.Processing(lastPartialText.ifBlank { Processing... })
 }

 override fun onError(error: Int) {
 val message = when (error) {
 SpeechRecognizer.ERROR_AUDIO -> Audio recording error
 SpeechRecognizer.ERROR_CLIENT -> Client speech recognition error
 SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Microphone permission required
 SpeechRecognizer.ERROR_NETWORK -> Network connection error
 SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Network timeout
 SpeechRecognizer.ERROR_NO_MATCH -> No speech recognized. Please try again or type below.
 SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Recognition service busy. Retrying...
 SpeechRecognizer.ERROR_SERVER -> Recognition server error
 SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> No speech detected within time limit
 else -> Recognition error ()
 }
 _listeningState.value = VoiceState.Error(message)
 destroyRecognizer()
 }

 override fun onResults(results: Bundle?) {
 val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
 val spokenText = matches?.firstOrNull() ?: lastPartialText
 destroyRecognizer()
 if (spokenText.isNotBlank()) {
 processRecognizedText(spokenText, cachedSnapshots)
 } else {
 _listeningState.value = VoiceState.Error(No speech recognized. Try speaking again.)
 }
 }

 override fun onPartialResults(partialResults: Bundle?) {
 val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
 val text = partials?.firstOrNull()
 if (!text.isNullOrBlank()) {
 lastPartialText = text
 _listeningState.value = VoiceState.Hearing(text)
 }
 }

 override fun onEvent(eventType: Int, params: Bundle?) {}
 })

 val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
 putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
 putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
 putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
 putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
 }

 recognizer.startListening(intent)
 _listeningState.value = VoiceState.Listening
 } catch (e: Exception) {
 _listeningState.value = VoiceState.Error(Failed to initialize speech recognizer: )
 destroyRecognizer()
 }
 }
 }

 override fun stopListening() {
 mainHandler.post {
 try {
 speechRecognizer?.stopListening()
 } catch (_: Exception) {}
 }
 }

 override fun processText(text: String, candidateSnapshots: List<ContextSnapshot>?) {
 processRecognizedText(text, candidateSnapshots ?: cachedSnapshots)
 }

 override fun resetState() {
 destroyRecognizer()
 lastPartialText = 
 _listeningState.value = VoiceState.Idle
 }

 private fun processRecognizedText(spokenText: String, candidateSnapshots: List<ContextSnapshot>?) {
 _listeningState.value = VoiceState.Processing(spokenText)
 coroutineScope.launch {
 try {
 // 1. Parse command via VoiceCommandProcessor
 val parsed = voiceCommandProcessor.processCommand(spokenText)

 // 2. Fetch available snapshots if not provided
 val snapshots = candidateSnapshots ?: try {
 snapshotRepository.getSnapshots().first()
 } catch (_: Exception) {
 emptyList()
 }

 // 3. Resolve intent & context
 when (parsed.intent) {
 VoiceIntent.RESUME_CONTEXT -> {
 when (val matchResult = contextMatcher.match(parsed.contextName, snapshots)) {
 is ContextMatchResult.ExactMatch -> {
 _listeningState.value = VoiceState.MatchFound(parsed, matchResult.snapshot)
 }
 is ContextMatchResult.SingleMatch -> {
 _listeningState.value = VoiceState.MatchFound(parsed, matchResult.snapshot)
 }
 is ContextMatchResult.AmbiguousMatch -> {
 _listeningState.value = VoiceState.Ambiguous(parsed, matchResult.candidates)
 }
 is ContextMatchResult.NoneSpecified -> {
 if (matchResult.availableSnapshots.size == 1) {
 _listeningState.value = VoiceState.MatchFound(parsed, matchResult.availableSnapshots.first())
 } else {
 _listeningState.value = VoiceState.Ambiguous(parsed, matchResult.availableSnapshots)
 }
 }
 is ContextMatchResult.NoMatch -> {
 _listeningState.value = VoiceState.Unrecognized(
 spokenText = spokenText,
 reason = No context found matching ""
 )
 }
 }
 }

 VoiceIntent.SAVE_CONTEXT -> {
 _listeningState.value = VoiceState.IntentRecognized(parsed)
 }

 VoiceIntent.LIST_CONTEXTS -> {
 _listeningState.value = VoiceState.IntentRecognized(parsed)
 }

 VoiceIntent.DELETE_CONTEXT -> {
 val match = contextMatcher.match(parsed.contextName, snapshots)
 val targetSnapshot = when (match) {
 is ContextMatchResult.ExactMatch -> match.snapshot
 is ContextMatchResult.SingleMatch -> match.snapshot
 else -> null
 }
 _listeningState.value = VoiceState.IntentRecognized(
 command = parsed.copy(requiresConfirmation = true),
 snapshot = targetSnapshot
 )
 }

 VoiceIntent.UNKNOWN -> {
 _listeningState.value = VoiceState.Unrecognized(
 spokenText = spokenText,
 reason = parsed.note ?: Command not recognized. Try: "Resume Project Alpha"
 )
 }
 }
 } catch (e: Exception) {
 _listeningState.value = VoiceState.Error(Error processing command: )
 }
 }
 }

 private fun destroyRecognizer() {
 mainHandler.post {
 try {
 speechRecognizer?.cancel()
 speechRecognizer?.destroy()
 } catch (_: Exception) {}
 speechRecognizer = null
 }
 }
}
