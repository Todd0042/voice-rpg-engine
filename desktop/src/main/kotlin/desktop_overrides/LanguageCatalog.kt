package com.voicerpg.engine.model

import java.util.Locale

data class RegionOption(
    val code: String,
    val displayName: String
)

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val regions: List<RegionOption> = emptyList(),
    val isFeatured: Boolean = false
)

object LanguageCatalog {
    private val SUPPORTED_CODES = listOf("en", "es", "de", "fr", "pt", "it", "ja", "ko", "zh")

    val SUPPORTED_LANGUAGES: List<LanguageOption> by lazy {
        SUPPORTED_CODES.map { code ->
            val locale = Locale(code)
            val enName = locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
            val nativeName = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
            LanguageOption(
                code = code,
                displayName = enName,
                nativeName = nativeName,
                regions = listOf(RegionOption("", "Default")),
                isFeatured = true
            )
        }
    }

    fun getLanguage(code: String): LanguageOption =
        SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code.take(2), ignoreCase = true) } ?: SUPPORTED_LANGUAGES.first()

    fun getRegionsForLanguage(langCode: String): List<RegionOption> = getLanguage(langCode).regions

    fun isValidLanguage(code: String): Boolean = SUPPORTED_LANGUAGES.any { it.code.equals(code.take(2), ignoreCase = true) }
}
