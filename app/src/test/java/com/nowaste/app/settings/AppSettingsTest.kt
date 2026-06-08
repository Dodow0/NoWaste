package com.nowaste.app.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsTest {
    @Test
    fun smartParsingApiKeyMigratesFromRegularPreferencesToSensitivePreferences() {
        val regularPreferences = InMemorySharedPreferences()
        val sensitivePreferences = InMemorySharedPreferences()
        regularPreferences
            .edit()
            .putString("smart_parsing_api_key", "  sk-legacy  ")
            .apply()

        val settings = AppSettings(regularPreferences, sensitivePreferences)

        assertEquals("sk-legacy", settings.smartParsingApiKey)
        assertEquals("sk-legacy", settings.state.value.smartParsingApiKey)
        assertFalse(regularPreferences.contains("smart_parsing_api_key"))
        assertEquals("sk-legacy", sensitivePreferences.getString("smart_parsing_api_key", null))

        settings.dispose()
    }

    @Test
    fun clearingSmartParsingApiKeyRemovesLegacyAndSensitiveValues() {
        val regularPreferences = InMemorySharedPreferences()
        val sensitivePreferences = InMemorySharedPreferences()
        regularPreferences
            .edit()
            .putString("smart_parsing_api_key", "sk-legacy")
            .apply()
        sensitivePreferences
            .edit()
            .putString("smart_parsing_api_key", "sk-sensitive")
            .apply()

        val settings = AppSettings(regularPreferences, sensitivePreferences)

        settings.smartParsingApiKey = ""

        assertEquals("", settings.smartParsingApiKey)
        assertFalse(regularPreferences.contains("smart_parsing_api_key"))
        assertFalse(sensitivePreferences.contains("smart_parsing_api_key"))

        settings.dispose()
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean =
        values.containsKey(key)

    override fun edit(): SharedPreferences.Editor =
        Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners -= listener
    }

    private inner class Editor : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var shouldClear = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            applyUpdate(key, value)

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor =
            applyUpdate(key, values?.toSet())

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            applyUpdate(key, value)

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            applyUpdate(key, value)

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            applyUpdate(key, value)

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            applyUpdate(key, value)

        override fun remove(key: String): SharedPreferences.Editor = apply {
            removals += key
            updates -= key
        }

        override fun clear(): SharedPreferences.Editor = apply {
            shouldClear = true
            updates.clear()
            removals.clear()
        }

        override fun commit(): Boolean {
            val changedKeys = mutableSetOf<String>()
            if (shouldClear) {
                changedKeys += values.keys
                values.clear()
            }
            removals.forEach { key ->
                if (values.containsKey(key)) {
                    values.remove(key)
                    changedKeys += key
                }
            }
            updates.forEach { (key, value) ->
                if (value == null) {
                    if (values.containsKey(key)) {
                        values.remove(key)
                        changedKeys += key
                    }
                } else if (values[key] != value) {
                    values[key] = value
                    changedKeys += key
                }
            }
            changedKeys.forEach { key ->
                listeners.forEach { listener ->
                    listener.onSharedPreferenceChanged(this@InMemorySharedPreferences, key)
                }
            }
            return true
        }

        override fun apply() {
            commit()
        }

        private fun applyUpdate(key: String, value: Any?): SharedPreferences.Editor = apply {
            updates[key] = value
            removals -= key
        }
    }
}
