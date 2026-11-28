package android.content.res

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class AssetFileDescriptor(val file: File) : AutoCloseable {
    val fileDescriptor: File get() = file
    val startOffset: Long = 0L
    val length: Long get() = file.length()
    override fun close() {}
}

class AssetManager(var rootDir: File) {
    fun open(fileName: String): InputStream = FileInputStream(File(rootDir, fileName))

    fun openFd(fileName: String): AssetFileDescriptor {
        val f = File(rootDir, fileName)
        if (!f.isFile) throw FileNotFoundException(f.absolutePath)
        return AssetFileDescriptor(f)
    }

    fun list(path: String): Array<String>? {
        val dir = File(rootDir, path)
        return if (dir.isDirectory) dir.list() else null
    }
}

class Configuration(
    var screenWidthDp: Int = 460,
    var screenHeightDp: Int = 818,
    var orientation: Int = ORIENTATION_PORTRAIT
) {
    companion object {
        const val ORIENTATION_PORTRAIT: Int = 1
        const val ORIENTATION_LANDSCAPE: Int = 2
    }
}
