package android.media

import java.io.File
import java.util.Collections
import javax.sound.sampled.AudioFormat as JvmAudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class AudioAttributes {
    class Builder {
        fun setUsage(usage: Int): Builder = this
        fun setContentType(contentType: Int): Builder = this
        fun build(): AudioAttributes = AudioAttributes()
    }

    companion object {
        const val USAGE_MEDIA: Int = 1
        const val USAGE_GAME: Int = 14
        const val CONTENT_TYPE_MUSIC: Int = 2
        const val CONTENT_TYPE_SPEECH: Int = 1
        const val CONTENT_TYPE_SONIFICATION: Int = 4
    }
}

class AudioFormat {
    class Builder {
        fun setEncoding(encoding: Int): Builder = this
        fun setSampleRate(sampleRateInHz: Int): Builder = this
        fun setChannelMask(channelMask: Int): Builder = this
        fun build(): AudioFormat = AudioFormat()
    }

    companion object {
        const val ENCODING_PCM_16BIT: Int = 2
        const val CHANNEL_OUT_MONO: Int = 4
    }
}

class AudioFocusRequest {
    class Builder(val focusGain: Int) {
        fun setAudioAttributes(attributes: AudioAttributes): Builder = this
        fun setAcceptsDelayedFocusGain(acceptsDelayedFocusGain: Boolean): Builder = this
        fun setOnAudioFocusChangeListener(listener: AudioManager.OnAudioFocusChangeListener): Builder = this
        fun build(): AudioFocusRequest = AudioFocusRequest()
    }
}

class AudioManager {
    fun interface OnAudioFocusChangeListener {
        fun onAudioFocusChange(focusChange: Int)
    }

    fun requestAudioFocus(focusRequest: AudioFocusRequest): Int = AUDIOFOCUS_REQUEST_GRANTED
    fun requestAudioFocus(l: OnAudioFocusChangeListener, streamType: Int, durationHint: Int): Int = AUDIOFOCUS_REQUEST_GRANTED
    fun abandonAudioFocusRequest(focusRequest: AudioFocusRequest): Int = AUDIOFOCUS_REQUEST_GRANTED
    fun abandonAudioFocus(l: OnAudioFocusChangeListener): Int = AUDIOFOCUS_REQUEST_GRANTED
    fun adjustStreamVolume(streamType: Int, direction: Int, flags: Int) {}

    companion object {
        const val AUDIOFOCUS_GAIN: Int = 1
        const val AUDIOFOCUS_LOSS: Int = -1
        const val AUDIOFOCUS_LOSS_TRANSIENT: Int = -2
        const val AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: Int = -3
        const val AUDIOFOCUS_REQUEST_GRANTED: Int = 1
        const val STREAM_MUSIC: Int = 3
        const val STREAM_NOTIFICATION: Int = 5
        const val STREAM_SYSTEM: Int = 1
        const val ADJUST_MUTE: Int = -100
        const val ADJUST_UNMUTE: Int = 100
    }
}

class AudioTrack private constructor() {
    class Builder {
        fun setAudioAttributes(attributes: AudioAttributes): Builder = this
        fun setAudioFormat(format: AudioFormat): Builder = this
        fun setBufferSizeInBytes(bufferSizeInBytes: Int): Builder = this
        fun setTransferMode(mode: Int): Builder = this
        fun build(): AudioTrack = AudioTrack()
    }

    private var line: SourceDataLine? = null
    private val pendingBytes = mutableListOf<Byte>()

    fun play() {
        runCatching {
            val fmt = JvmAudioFormat(22050f, 16, 1, true, false)
            val sdl = AudioSystem.getSourceDataLine(fmt)
            sdl.open(fmt)
            sdl.start()
            line = sdl
            if (pendingBytes.isNotEmpty()) {
                val arr = pendingBytes.toByteArray()
                pendingBytes.clear()
                sdl.write(arr, 0, arr.size)
            }
        }
    }

    fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        val bytes = ByteArray(sizeInShorts * 2)
        for (i in 0 until sizeInShorts) {
            val sample = audioData[offsetInShorts + i].toInt()
            bytes[i * 2] = (sample and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        val sdl = line
        if (sdl != null && sdl.isOpen) {
            sdl.write(bytes, 0, bytes.size)
        } else {
            for (b in bytes) pendingBytes.add(b)
        }
        return sizeInShorts
    }

    fun stop() {
        runCatching {
            line?.drain()
            line?.stop()
        }
    }

    fun release() {
        runCatching {
            line?.close()
            line = null
        }
    }

    companion object {
        const val MODE_STREAM: Int = 1
        const val MODE_STATIC: Int = 0
    }
}

class MediaPlayer {
    var isLooping: Boolean = false
    private var sourceFile: File? = null
    private var volume: Float = 0.55f
    private var process: Process? = null
    private var onCompletion: ((MediaPlayer) -> Unit)? = null

    val isPlaying: Boolean
        get() = process?.isAlive == true

    init {
        activePlayers.add(this)
    }

    fun setAudioAttributes(attributes: AudioAttributes) {}

    fun setDataSource(fd: File, offset: Long, length: Long) {
        sourceFile = fd
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        volume = leftVolume.coerceIn(0f, 1f)
    }

    fun setOnCompletionListener(listener: (MediaPlayer) -> Unit) {
        onCompletion = listener
    }

    fun prepare() {}

    fun start() {
        stopProcess()
        val file = sourceFile ?: return
        if (!file.isFile) return
        val volPct = (volume * 100f).toInt().coerceIn(0, 100)
        val cmd = mutableListOf("ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", "-volume", volPct.toString())
        if (isLooping) {
            cmd.add("-loop")
            cmd.add("0")
        }
        cmd.add(file.absolutePath)
        runCatching {
            val p = ProcessBuilder(cmd).start()
            process = p
            Thread({
                val code = runCatching { p.waitFor() }.getOrDefault(-1)
                if (process === p) {
                    process = null
                    if (code == 0 && !isLooping) {
                        onCompletion?.invoke(this)
                    }
                }
            }, "MediaPlayer-Wait").apply { isDaemon = true }.start()
        }
    }

    fun pause() {
        stopProcess()
    }

    fun stop() {
        stopProcess()
    }

    fun reset() {
        stopProcess()
        sourceFile = null
        isLooping = false
    }

    fun release() {
        stopProcess()
        activePlayers.remove(this)
    }

    private fun stopProcess() {
        val p = process
        process = null
        runCatching { p?.destroyForcibly() }
    }

    companion object {
        private val activePlayers: MutableSet<MediaPlayer> = Collections.synchronizedSet(mutableSetOf())

        init {
            Runtime.getRuntime().addShutdownHook(Thread { stopAll() })
        }

        fun stopAll() {
            synchronized(activePlayers) {
                activePlayers.toList().forEach { it.stopProcess() }
            }
        }
    }
}
