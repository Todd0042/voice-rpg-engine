package androidx.core.util

import java.io.File
import java.io.FileOutputStream

class AtomicFile(private val baseName: File) {
    fun startWrite(): FileOutputStream {
        baseName.parentFile?.mkdirs()
        return FileOutputStream(baseName)
    }

    fun finishWrite(str: FileOutputStream?) {
        str?.flush()
        str?.close()
    }

    fun failWrite(str: FileOutputStream?) {
        runCatching { str?.close() }
    }

    fun readFully(): ByteArray = baseName.readBytes()

    fun delete(): Boolean = baseName.delete()
}
