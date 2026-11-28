package android.speech.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import java.io.File
import java.util.Collections
import java.util.Locale

class Voice(
    val name: String,
    val locale: Locale = Locale.US,
    val quality: Int = 400,
    val latency: Int = 200,
    val isNetworkConnectionRequired: Boolean = false,
    val features: Set<String>? = emptySet()
)

abstract class UtteranceProgressListener {
    abstract fun onStart(utteranceId: String?)
    abstract fun onDone(utteranceId: String?)
    open fun onStop(utteranceId: String?, interrupted: Boolean) {}
    abstract fun onError(utteranceId: String?)
    open fun onError(utteranceId: String?, errorCode: Int) {}
}

class TextToSpeech(context: Context?, private val listener: OnInitListener?) {

    interface OnInitListener {
        fun onInit(status: Int)
    }

    object Engine {
        const val ACTION_INSTALL_TTS_DATA: String = "android.speech.tts.engine.INSTALL_TTS_DATA"
        const val KEY_FEATURE_NOT_INSTALLED: String = "notInstalled"
        const val KEY_PARAM_VOLUME: String = "volume"
    }

    private val modelPaths: Map<String, File> = discoverModels()
    val voices: Set<Voice> = modelPaths.keys.map { Voice(name = it, locale = Locale.US) }.toSet()
    var voice: Voice? = voices.firstOrNull()

    private var speechRate: Float = 1.0f
    private var pitch: Float = 1.0f
    private var progressListener: UtteranceProgressListener? = null
    @Volatile private var activeProcess: Process? = null
    @Volatile private var activeUtteranceId: String? = null
    @Volatile private var stoppedManually: Boolean = false

    init {
        activeEngines.add(this)
        listener?.onInit(if (modelPaths.isNotEmpty() && File("/usr/bin/piper").isFile) SUCCESS else ERROR)
    }

    fun setLanguage(loc: Locale): Int = LANG_AVAILABLE
    fun isLanguageAvailable(loc: Locale): Int = LANG_AVAILABLE
    fun setAudioAttributes(audioAttributes: AudioAttributes): Int = SUCCESS

    fun setSpeechRate(speechRate: Float): Int {
        this.speechRate = speechRate.coerceIn(0.5f, 2.0f)
        return SUCCESS
    }

    fun setPitch(pitch: Float): Int {
        this.pitch = pitch.coerceIn(0.5f, 2.0f)
        return SUCCESS
    }

    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener): Int {
        progressListener = listener
        return SUCCESS
    }

    fun speak(text: CharSequence, queueMode: Int, params: Bundle?, utteranceId: String?): Int {
        val modelFile = voice?.let { modelPaths[it.name] } ?: modelPaths.values.firstOrNull() ?: return ERROR
        val cleanText = text.toString().trim()
        if (cleanText.isEmpty()) return ERROR

        stop()
        stoppedManually = false
        activeUtteranceId = utteranceId
        val lengthScale = (1.0f / speechRate.coerceIn(0.5f, 2.0f))
        val script = "printf '%s' \"\$1\" | /usr/bin/piper --model \"\$2\" --length_scale \"\$3\" --output-raw 2>/dev/null | pw-play --rate 22050 --channels 1 --format s16 - 2>/dev/null"

        Thread({
            progressListener?.onStart(utteranceId)
            val proc = runCatching {
                ProcessBuilder("bash", "-c", script, "piper-tts", cleanText, modelFile.absolutePath, lengthScale.toString()).start()
            }.getOrNull()
            if (proc == null) {
                progressListener?.onError(utteranceId, ERROR)
                return@Thread
            }
            activeProcess = proc
            val exitCode = runCatching { proc.waitFor() }.getOrDefault(-1)
            if (activeProcess === proc) {
                activeProcess = null
            }
            if (stoppedManually) {
                progressListener?.onStop(utteranceId, true)
            } else if (exitCode == 0) {
                progressListener?.onDone(utteranceId)
            } else {
                progressListener?.onError(utteranceId, ERROR)
            }
        }, "PiperTTS").apply { isDaemon = true }.start()

        return SUCCESS
    }

    fun stop(): Int {
        stoppedManually = true
        val p = activeProcess
        activeProcess = null
        runCatching {
            p?.descendants()?.forEach { it.destroyForcibly() }
            p?.destroyForcibly()
        }
        return SUCCESS
    }

    fun shutdown() {
        stop()
        activeEngines.remove(this)
    }

    companion object {
        const val SUCCESS: Int = 0
        const val ERROR: Int = -1
        const val QUEUE_FLUSH: Int = 0
        const val LANG_AVAILABLE: Int = 0
        const val LANG_MISSING_DATA: Int = -1
        const val LANG_NOT_SUPPORTED: Int = -2

        private val activeEngines: MutableSet<TextToSpeech> = Collections.synchronizedSet(mutableSetOf())

        init {
            Runtime.getRuntime().addShutdownHook(Thread { stopAll() })
        }

        fun stopAll() {
            synchronized(activeEngines) {
                activeEngines.toList().forEach { it.stop() }
            }
        }

        private fun discoverModels(): Map<String, File> {
            val home = System.getProperty("user.home")
            val dirs = listOf(
                File(home, ".local/share/piper-voices"),
                File(home, "piper/voices")
            )
            val out = linkedMapOf<String, File>()
            for (dir in dirs) {
                if (!dir.isDirectory) continue
                dir.walkTopDown()
                    .filter { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
                    .sortedBy { it.name }
                    .forEach { file -> out.putIfAbsent(file.nameWithoutExtension, file) }
            }
            return out
        }
    }
}
