package com.voicerpg.engine.ui.creation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.model.SelfieFilterConfig
import java.io.File

class TakeSelfiePreviewContract : ActivityResultContracts.TakePicturePreview()

object SelfiePortraitProcessor {
    const val PORTRAIT_SIZE: Int = 512
    const val CUSTOM_PORTRAIT_FILENAME: String = "custom_hero_portrait.jpg"

    fun loadFromUri(context: Context, uri: Uri): Bitmap? = null

    fun getCustomPortraitFilename(slot: Int): String =
        if (slot == SaveManager.STORY_MODE_SLOT) "custom_hero_portrait_story.jpg"
        else "custom_hero_portrait_slot_$slot.jpg"

    fun saveCustomPortrait(context: Context, bitmap: Bitmap, slot: Int = SaveManager.DEFAULT_SLOT): File =
        File(context.filesDir, getCustomPortraitFilename(slot))

    fun getCustomPortraitFile(context: Context, slot: Int = SaveManager.DEFAULT_SLOT): File? {
        val f = File(context.filesDir, getCustomPortraitFilename(slot))
        return if (f.exists()) f else null
    }

    fun deleteCustomPortrait(context: Context, slot: Int = SaveManager.DEFAULT_SLOT): Boolean =
        getCustomPortraitFile(context, slot)?.delete() ?: false
}

@Composable
fun SelfieFilterDialog(
    rawBitmap: Bitmap,
    filterConfig: SelfieFilterConfig,
    onDismiss: () -> Unit,
    onSavePortrait: (Bitmap) -> Unit
) {
    onDismiss()
}
