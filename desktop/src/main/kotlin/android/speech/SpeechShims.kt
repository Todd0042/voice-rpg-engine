package android.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle

interface RecognitionListener {
    fun onReadyForSpeech(params: Bundle?) {}
    fun onBeginningOfSpeech() {}
    fun onRmsChanged(rmsdB: Float) {}
    fun onBufferReceived(buffer: ByteArray?) {}
    fun onEndOfSpeech() {}
    fun onError(error: Int) {}
    fun onResults(results: Bundle?) {}
    fun onPartialResults(partialResults: Bundle?) {}
    fun onEvent(eventType: Int, params: Bundle?) {}
}

object RecognizerIntent {
    const val ACTION_RECOGNIZE_SPEECH: String = "android.speech.action.RECOGNIZE_SPEECH"
    const val EXTRA_LANGUAGE_MODEL: String = "android.speech.extra.LANGUAGE_MODEL"
    const val LANGUAGE_MODEL_FREE_FORM: String = "free_form"
    const val EXTRA_PARTIAL_RESULTS: String = "android.speech.extra.PARTIAL_RESULTS"
    const val EXTRA_MAX_RESULTS: String = "android.speech.extra.MAX_RESULTS"
    const val EXTRA_LANGUAGE: String = "android.speech.extra.LANGUAGE"
    const val EXTRA_LANGUAGE_PREFERENCE: String = "android.speech.extra.LANGUAGE_PREFERENCE"
    const val EXTRA_PREFER_OFFLINE: String = "android.speech.extra.PREFER_OFFLINE"
    const val EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS: String = "android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS"
    const val EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS: String = "android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS"
    const val EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS: String = "android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS"
}

class SpeechRecognizer {
    fun setRecognitionListener(listener: RecognitionListener) {}
    fun startListening(recognizerIntent: Intent) {}
    fun stopListening() {}
    fun cancel() {}
    fun destroy() {}

    companion object {
        const val ERROR_AUDIO: Int = 3
        const val ERROR_CLIENT: Int = 5
        const val ERROR_INSUFFICIENT_PERMISSIONS: Int = 9
        const val ERROR_NETWORK: Int = 2
        const val ERROR_NETWORK_TIMEOUT: Int = 1
        const val ERROR_NO_MATCH: Int = 7
        const val ERROR_RECOGNIZER_BUSY: Int = 8
        const val ERROR_SERVER: Int = 4
        const val ERROR_SPEECH_TIMEOUT: Int = 6
        const val RESULTS_RECOGNITION: String = "results_recognition"

        fun isRecognitionAvailable(context: Context): Boolean = false
        fun isOnDeviceRecognitionAvailable(context: Context): Boolean = false
        fun createSpeechRecognizer(context: Context): SpeechRecognizer = SpeechRecognizer()
        fun createOnDeviceSpeechRecognizer(context: Context): SpeechRecognizer = SpeechRecognizer()
    }
}
