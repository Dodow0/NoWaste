package com.nowaste.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.nowaste.app.domain.DefaultFoodCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val reminderHour: Int = AppSettings.DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = AppSettings.DEFAULT_REMINDER_MINUTE,
    val nearExpiryDays: Int = AppSettings.DEFAULT_NEAR_EXPIRY_DAYS,
    val categoryTags: List<String> = AppSettings.DEFAULT_CATEGORY_TAGS,
)

class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val stateFlow = MutableStateFlow(readState())
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        stateFlow.value = readState()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    val state: StateFlow<SettingsState> = stateFlow.asStateFlow()

    var reminderHour: Int
        get() = preferences.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) {
            preferences.edit().putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)).apply()
        }

    var reminderMinute: Int
        get() = preferences.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
        set(value) {
            preferences.edit().putInt(KEY_REMINDER_MINUTE, value.coerceIn(0, 59)).apply()
        }

    var nearExpiryDays: Int
        get() = preferences.getInt(KEY_NEAR_EXPIRY_DAYS, DEFAULT_NEAR_EXPIRY_DAYS)
        set(value) {
            preferences.edit()
                .putInt(KEY_NEAR_EXPIRY_DAYS, value.coerceIn(MIN_NEAR_EXPIRY_DAYS, MAX_NEAR_EXPIRY_DAYS))
                .apply()
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
            preferences.edit()
                .putString(KEY_CATEGORY_TAGS, tags.joinToString(TAG_SEPARATOR))
                .apply()
        }

    fun updateReminderTime(hour: Int, minute: Int) {
        preferences.edit()
            .putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun addCategoryTag(tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isBlank()) return
        categoryTags = categoryTags + normalizedTag
    }

    fun deleteCategoryTag(tag: String) {
        categoryTags = categoryTags.filterNot { it == tag }
    }

    private fun readState(): SettingsState =
        SettingsState(
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            nearExpiryDays = nearExpiryDays,
            categoryTags = categoryTags,
        )

    fun dispose() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val DEFAULT_REMINDER_MINUTE = 0
        const val DEFAULT_NEAR_EXPIRY_DAYS = 3
        const val MIN_NEAR_EXPIRY_DAYS = 1
        const val MAX_NEAR_EXPIRY_DAYS = 30
        val DEFAULT_CATEGORY_TAGS = DefaultFoodCategories

        private const val PREFERENCES_NAME = "nowaste_settings"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_NEAR_EXPIRY_DAYS = "near_expiry_days"
        private const val KEY_CATEGORY_TAGS = "category_tags"
        private const val TAG_SEPARATOR = "\n"
    }
}
