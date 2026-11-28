package androidx.activity.result

class ActivityResultLauncher<I> {
    fun launch(input: I? = null) {}
}

class PickVisualMediaRequest

fun PickVisualMediaRequest(mediaType: Any): PickVisualMediaRequest = PickVisualMediaRequest()
