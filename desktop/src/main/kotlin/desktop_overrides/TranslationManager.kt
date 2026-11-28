package com.voicerpg.engine.localization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

enum class ModelDownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

object TranslationManager {
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _currentRegion = MutableStateFlow("")
    val currentRegion: StateFlow<String> = _currentRegion.asStateFlow()

    private val _downloadStatus = MutableStateFlow(ModelDownloadStatus.IDLE)
    val downloadStatus: StateFlow<ModelDownloadStatus> = _downloadStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadTargetName = MutableStateFlow("")
    val downloadTargetName: StateFlow<String> = _downloadTargetName.asStateFlow()

    private val _translationVersion = MutableStateFlow(0)
    val translationVersion: StateFlow<Int> = _translationVersion.asStateFlow()

    private var appContext: Context? = null
    private val assetDictionaries = ConcurrentHashMap<String, Map<String, String>>()

    fun initialize(context: Context? = null) {
        appContext = context?.applicationContext
        loadAssetDictionary(_currentLanguage.value)
    }

    fun dismissDownloadBanner() {
        _downloadStatus.value = ModelDownloadStatus.IDLE
    }

    fun loadAssetDictionary(lang: String) {
        val ctx = appContext ?: return
        val clean = lang.lowercase().trim().take(2)
        if (clean == "en" || clean.isBlank() || assetDictionaries.containsKey(clean)) return
        val merged = readDictionary({ ctx.assets.open(it) }, "game/localization/strings_$clean.json") +
            readDictionary({ ctx.assets.open(it) }, "game/translations/strings_$clean.json")
        if (merged.isNotEmpty()) {
            assetDictionaries[clean] = merged
            _translationVersion.value++
        }
    }

    private fun readDictionary(openAsset: (String) -> InputStream, path: String): Map<String, String> =
        try {
            openAsset(path).use { input ->
                val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                com.google.gson.Gson().fromJson<Map<String, String>>(input.reader(Charsets.UTF_8), type).orEmpty()
            }
        } catch (_: Throwable) {
            emptyMap()
        }

    suspend fun isModelInstalled(lang: String): Boolean = true

    fun setLanguageAndRegion(lang: String, region: String = "") {
        _currentLanguage.value = lang.lowercase().trim().take(2)
        _currentRegion.value = region.uppercase().trim()
        loadAssetDictionary(_currentLanguage.value)
    }

    fun translate(text: String, targetLang: String = _currentLanguage.value): String {
        val lang = targetLang.lowercase().trim().take(2)
        if (lang == "en" || lang.isBlank()) return text
        return assetDictionaries[lang]?.get(text) ?: text
    }

    fun translateKey(key: String, defaultFallback: String): String = translate(defaultFallback)
}
