package android.content

import android.content.res.AssetManager
import java.io.File

class Intent(var action: String = "") {
    var flags: Int = 0
    var data: android.net.Uri? = null

    fun putExtra(name: String, value: Any?): Intent = this

    companion object {
        const val FLAG_ACTIVITY_NEW_TASK: Int = 0x10000000
    }
}

open class Context(
    var assetsDir: File = File("app/src/main/assets"),
    var filesDir: File = File(System.getProperty("user.home"), ".local/share/voicerpg-desktop/default")
) {
    val assets: AssetManager get() = AssetManager(assetsDir)
    val applicationContext: Context get() = this
    val cacheDir: File get() = File(filesDir, "cache").apply { mkdirs() }

    fun getSystemService(name: String): Any? = null
    fun startActivity(intent: Intent) {}

    companion object {
        const val AUDIO_SERVICE: String = "audio"
    }
}
