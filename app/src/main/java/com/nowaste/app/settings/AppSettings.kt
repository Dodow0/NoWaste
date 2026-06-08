package com.nowaste.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nowaste.app.domain.AppTheme
import com.nowaste.app.domain.DefaultFoodCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val reminderHour: Int = AppSettings.DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = AppSettings.DEFAULT_REMINDER_MINUTE,
    val nearExpiryDays: Int = AppSettings.DEFAULT_NEAR_EXPIRY_DAYS,
    val categoryTags: List<String> = AppSettings.DEFAULT_CATEGORY_TAGS,
    val smartParsingEnabled: Boolean = false,
    val smartParsingApiUrl: String = "",
    val smartParsingApiKey: String = "",
    val smartParsingModel: String = "",
    val theme: AppTheme = AppTheme.FOLLOW_SYSTEM,
)

class AppSettings internal constructor(
    private val preferences: SharedPreferences,
    private val sensitivePreferences: SharedPreferences,
) {
    private val stateFlow = MutableStateFlow(readState())
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshState()
    }
    private val sensitiveListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshState()
    }

    constructor(context: Context) : this(
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        sensitivePreferences = context.getSharedPreferences(SENSITIVE_PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
        sensitivePreferences.registerOnSharedPreferenceChangeListener(sensitiveListener)
    }

    val state: StateFlow<SettingsState> = stateFlow.asStateFlow()

    var reminderHour: Int
        get() = preferences.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) {
            updatePreferences {
                putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23))
            }
        }

    var reminderMinute: Int
        get() = preferences.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
        set(value) {
            updatePreferences {
                putInt(KEY_REMINDER_MINUTE, value.coerceIn(0, 59))
            }
        }

    var nearExpiryDays: Int
        get() = preferences.getInt(KEY_NEAR_EXPIRY_DAYS, DEFAULT_NEAR_EXPIRY_DAYS)
        set(value) {
            updatePreferences {
                putInt(KEY_NEAR_EXPIRY_DAYS, value.coerceIn(MIN_NEAR_EXPIRY_DAYS, MAX_NEAR_EXPIRY_DAYS))
            }
        }

    var categoryTags: List<String>
        get() {
            val rawTags = preferences.getString(KEY_CATEGORY_TAGS, null)
                ?: return DEFAULT_CATEGORY_TAGS
            return rawTags
                .split(TAG_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
        set(value) {
            val tags = value
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            updatePreferences {
                putString(KEY_CATEGORY_TAGS, tags.joinToString(TAG_SEPARATOR))
            }
        }

    var smartParsingEnabled: Boolean
        get() = preferences.getBoolean(KEY_SMART_PARSING_ENABLED, false)
        set(value) {
            updatePreferences {
                putBoolean(KEY_SMART_PARSING_ENABLED, value)
            }
        }

    var smartParsingApiUrl: String
        get() = preferences.getString(KEY_SMART_PARSING_API_URL, "").orEmpty()
        set(value) {
            updatePreferences {
                putString(KEY_SMART_PARSING_API_URL, value.trim())
            }
        }

    var smartParsingApiKey: String
        get() = sensitivePreferences.getString(KEY_SMART_PARSING_API_KEY, null)
            ?: migrateLegacySmartParsingApiKey()
        set(value) {
            val cleanValue = value.trim()
            if (cleanValue.isBlank() && preferences.contains(KEY_SMART_PARSING_API_KEY)) {
                updatePreferences {
                    remove(KEY_SMART_PARSING_API_KEY)
                }
            }
            updateSensitivePreferences {
                if (cleanValue.isBlank()) {
                    remove(KEY_SMART_PARSING_API_KEY)
                } else {
                    putString(KEY_SMART_PARSING_API_KEY, cleanValue)
                }
            }
            if (cleanValue.isNotBlank() && preferences.contains(KEY_SMART_PARSING_API_KEY)) {
                updatePreferences {
                    remove(KEY_SMART_PARSING_API_KEY)
                }
            }
        }

    var smartParsingModel: String
        get() = preferences.getString(KEY_SMART_PARSING_MODEL, "").orEmpty()
        set(value) {
            updatePreferences {
                putString(KEY_SMART_PARSING_MODEL, value.trim())
            }
        }

    var theme: AppTheme
        get() {
            val name = preferences.getString(KEY_THEME, AppTheme.FOLLOW_SYSTEM.name)
            return try {
                AppTheme.valueOf(name!!)
            } catch (e: Exception) {
                AppTheme.FOLLOW_SYSTEM
            }
        }
        set(value) {
            updatePreferences {
                putString(KEY_THEME, value.name)
            }
        }

    fun updateReminderTime(hour: Int, minute: Int) {
        updatePreferences {
            putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
            putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
        }
    }

    fun addCategoryTag(tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isBlank()) return
        categoryTags = categoryTags + normalizedTag
    }

    fun deleteCategoryTag(tag: String) {
        categoryTags = categoryTags.filterNot { it == tag }
    }

    fun moveCategoryTag(tag: String, direction: Int) {
        val tags = categoryTags.toMutableList()
        val fromIndex = tags.indexOf(tag)
        if (fromIndex == -1) return
        val toIndex = (fromIndex + direction).coerceIn(0, tags.lastIndex)
        if (fromIndex == toIndex) return
        tags.removeAt(fromIndex)
        tags.add(toIndex, tag)
        categoryTags = tags
    }

    private fun readState(): SettingsState =
        SettingsState(
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            nearExpiryDays = nearExpiryDays,
            categoryTags = categoryTags,
            smartParsingEnabled = smartParsingEnabled,
            smartParsingApiUrl = smartParsingApiUrl,
            smartParsingApiKey = smartParsingApiKey,
            smartParsingModel = smartParsingModel,
            theme = theme,
        )

    private fun updatePreferences(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit { block() }
        refreshState()
    }

    private fun updateSensitivePreferences(block: SharedPreferences.Editor.() -> Unit) {
        sensitivePreferences.edit { block() }
        refreshState()
    }

    private fun migrateLegacySmartParsingApiKey(): String {
        val legacyValue = preferences.getString(KEY_SMART_PARSING_API_KEY, "").orEmpty().trim()
        if (legacyValue.isNotBlank()) {
            sensitivePreferences.edit {
                putString(KEY_SMART_PARSING_API_KEY, legacyValue)
            }
            preferences.edit {
                remove(KEY_SMART_PARSING_API_KEY)
            }
        } else if (preferences.contains(KEY_SMART_PARSING_API_KEY)) {
            preferences.edit {
                remove(KEY_SMART_PARSING_API_KEY)
            }
        }
        return legacyValue
    }

    private fun refreshState() {
        stateFlow.value = readState()
    }

    fun dispose() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
        sensitivePreferences.unregisterOnSharedPreferenceChangeListener(sensitiveListener)
    }

    companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val DEFAULT_REMINDER_MINUTE = 0
        const val DEFAULT_NEAR_EXPIRY_DAYS = 3
        const val MIN_NEAR_EXPIRY_DAYS = 1
        const val MAX_NEAR_EXPIRY_DAYS = 30
        val DEFAULT_CATEGORY_TAGS = DefaultFoodCategories

        private const val PREFERENCES_NAME = "nowaste_settings"
        private const val SENSITIVE_PREFERENCES_NAME = "nowaste_sensitive_settings"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_NEAR_EXPIRY_DAYS = "near_expiry_days"
        private const val KEY_CATEGORY_TAGS = "category_tags"
        private const val KEY_SMART_PARSING_ENABLED = "smart_parsing_enabled"
        private const val KEY_SMART_PARSING_API_URL = "smart_parsing_api_url"
        private const val KEY_SMART_PARSING_API_KEY = "smart_parsing_api_key"
        private const val KEY_SMART_PARSING_MODEL = "smart_parsing_model"
        private const val KEY_THEME = "app_theme"
        private const val TAG_SEPARATOR = "\n"
    }
}
