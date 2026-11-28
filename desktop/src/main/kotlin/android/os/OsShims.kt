package android.os

class Bundle {
    private val floats = mutableMapOf<String, Float>()
    private val strings = mutableMapOf<String, String?>()

    fun putFloat(key: String, value: Float) {
        floats[key] = value
    }

    fun putString(key: String, value: String?) {
        strings[key] = value
    }

    fun getStringArrayList(key: String): ArrayList<String>? = null
}

object Build {
    object VERSION {
        const val SDK_INT: Int = 35
    }

    object VERSION_CODES {
        const val O: Int = 26
        const val S: Int = 31
    }
}
