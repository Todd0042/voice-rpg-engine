package android.graphics

import java.awt.image.BufferedImage
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.imageio.ImageIO

object Color {
    fun parseColor(colorString: String?): Int {
        if (colorString.isNullOrBlank()) throw IllegalArgumentException("Unknown color")
        val clean = colorString.trim()
        if (!clean.startsWith("#")) throw IllegalArgumentException("Unknown color: $colorString")
        val hex = clean.substring(1)
        val value = hex.toLong(16)
        return when (hex.length) {
            6 -> (0xFF000000L or value).toInt()
            8 -> value.toInt()
            else -> throw IllegalArgumentException("Unknown color: $colorString")
        }
    }

    fun red(color: Int): Int = (color ushr 16) and 0xFF
    fun green(color: Int): Int = (color ushr 8) and 0xFF
    fun blue(color: Int): Int = color and 0xFF
    fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
}

class Bitmap(val buffered: BufferedImage) {
    enum class Config { ARGB_8888 }
    enum class CompressFormat { JPEG, PNG }

    val width: Int get() = buffered.width
    val height: Int get() = buffered.height

    fun getPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
        buffered.getRGB(x, y, width, height, pixels, offset, stride)
    }

    fun setPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
        buffered.setRGB(x, y, width, height, pixels, offset, stride)
    }

    fun compress(format: CompressFormat, quality: Int, stream: OutputStream): Boolean {
        val ext = if (format == CompressFormat.PNG) "png" else "jpg"
        return ImageIO.write(buffered, ext, stream)
    }

    companion object {
        fun createBitmap(colors: IntArray, width: Int, height: Int, config: Config): Bitmap {
            val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            img.setRGB(0, 0, width, height, colors, 0, width)
            return Bitmap(img)
        }

        fun createBitmap(width: Int, height: Int, config: Config): Bitmap =
            Bitmap(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))
    }
}

object BitmapFactory {
    class Options {
        var inScaled: Boolean = true
        var inPremultiplied: Boolean = true
    }

    fun decodeStream(stream: InputStream?, outPadding: Any? = null, opts: Options? = null): Bitmap? {
        if (stream == null) return null
        return runCatching {
            val img = ImageIO.read(stream) ?: return null
            if (img.type == BufferedImage.TYPE_INT_ARGB) {
                Bitmap(img)
            } else {
                val argb = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
                val g = argb.createGraphics()
                g.drawImage(img, 0, 0, null)
                g.dispose()
                Bitmap(argb)
            }
        }.getOrNull()
    }

    fun decodeFile(pathName: String): Bitmap? = runCatching {
        FileInputStream(pathName).use { decodeStream(it) }
    }.getOrNull()
}
