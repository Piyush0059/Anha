package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechToTextManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onErrorState: (String) -> Unit = {},
    private val onListeningState: (Boolean) -> Unit = {}
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening(languageCode: String = "en") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorState("Voice input capability is not configured on this image.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechToTextManager)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningState(true)
        } catch (e: Exception) {
            onErrorState(e.message ?: "Failed to start speech capture.")
            onListeningState(false)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        onListeningState(false)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("AnhaSTT", "Ready for speech input.")
    }

    override fun onBeginningOfSpeech() {
        Log.d("AnhaSTT", "Beginning of speech input.")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Can be used for custom sound waves
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d("AnhaSTT", "Finished captured recording, processing...")
        onListeningState(false)
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition engine issue"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permissions missing"
            SpeechRecognizer.ERROR_NETWORK -> "Network sync issue"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recorder busy"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Recording timeout (no speech heard)"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech matching criteria"
            else -> "Speech recognition paused (Error code: $error)"
        }
        onErrorState(message)
        onListeningState(false)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        onResult(text)
        onListeningState(false)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        onPartialResult(text)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
