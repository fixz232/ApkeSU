package me.weishu.kernelsu.ui.util

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import me.weishu.kernelsu.R
import java.util.Locale

data class AppLanguage(
    val languageTag: String,
    @StringRes val displayNameRes: Int,
)

object AppLanguageManager {
    private const val PREFERENCES_NAME = "app_language"
    private const val LANGUAGE_TAG_KEY = "language_tag"
    private const val DEFAULT_LANGUAGE_TAG = "zh-CN"

    val supportedLanguages = listOf(
        AppLanguage(DEFAULT_LANGUAGE_TAG, R.string.language_chinese),
        AppLanguage("en", R.string.language_english),
        AppLanguage("fr", R.string.language_french),
        AppLanguage("ru", R.string.language_russian),
        AppLanguage("ja", R.string.language_japanese),
        AppLanguage("ko", R.string.language_korean),
        AppLanguage("es", R.string.language_spanish),
    )

    fun getSelectedLanguage(context: Context): AppLanguage {
        val preferences = preferences(context)
        val savedTag = preferences.getString(LANGUAGE_TAG_KEY, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val platformTag = platformLanguageTag(context)
            findSupportedLanguage(platformTag)?.let { language ->
                if (!savedTag.equals(language.languageTag, ignoreCase = true)) {
                    preferences.edit().putString(LANGUAGE_TAG_KEY, language.languageTag).apply()
                }
                return language
            }
        }

        findSupportedLanguage(savedTag)?.let { return it }
        return supportedLanguages.first()
    }

    fun wrapContext(context: Context): Context {
        val locale = Locale.forLanguageTag(getSelectedLanguage(context).languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    fun setSelectedLanguage(context: Context, languageTag: String): Boolean {
        val language = findSupportedLanguage(languageTag) ?: return false
        val previousLanguage = getSelectedLanguage(context)
        val preferences = preferences(context)
        if (!preferences.edit().putString(LANGUAGE_TAG_KEY, language.languageTag).commit()) {
            return false
        }

        val locale = Locale.forLanguageTag(language.languageTag)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!setPlatformLanguage(context, language.languageTag)) {
                preferences.edit().putString(LANGUAGE_TAG_KEY, previousLanguage.languageTag).commit()
                Locale.setDefault(Locale.forLanguageTag(previousLanguage.languageTag))
                return false
            }
        } else {
            updateLegacyResources(context, locale)
        }
        return true
    }

    fun syncPlatformLanguage(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val selectedTag = getSelectedLanguage(context).languageTag
        if (platformLanguageTag(context).equals(selectedTag, ignoreCase = true)) return
        setPlatformLanguage(context, selectedTag)
    }

    private fun preferences(context: Context): SharedPreferences {
        val storageContext = context.createDeviceProtectedStorageContext()
        return storageContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun findSupportedLanguage(languageTag: String?): AppLanguage? {
        if (languageTag.isNullOrBlank()) return null
        return supportedLanguages.firstOrNull {
            it.languageTag.equals(languageTag, ignoreCase = true)
        }
    }

    private fun platformLanguageTag(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return runCatching {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
        }.getOrNull()
    }

    private fun setPlatformLanguage(context: Context, languageTag: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return runCatching {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return false
            localeManager.applicationLocales = LocaleList.forLanguageTags(languageTag)
        }.isSuccess
    }

    @Suppress("DEPRECATION")
    private fun updateLegacyResources(context: Context, locale: Locale) {
        listOf(context, context.applicationContext).distinct().forEach { target ->
            val configuration = Configuration(target.resources.configuration).apply {
                setLocales(LocaleList(locale))
                setLayoutDirection(locale)
            }
            target.resources.updateConfiguration(configuration, target.resources.displayMetrics)
        }
    }
}
