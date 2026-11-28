package androidx.activity.result.contract

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest

abstract class ActivityResultContract<I, O>

object ActivityResultContracts {
    class PickVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
        object ImageOnly
    }
    open class TakePicturePreview : ActivityResultContract<Void?, Bitmap?>()
}
