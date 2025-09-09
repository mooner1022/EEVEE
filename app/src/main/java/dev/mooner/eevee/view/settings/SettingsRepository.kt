package dev.mooner.eevee.view.settings

import android.content.Context
import androidx.core.content.edit
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events

class SettingsRepository(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        return sharedPrefs.getBoolean(key, defaultValue)
    }

    fun setBooleanValue(key: String, value: Boolean) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putBoolean(key, value) }
    }

    fun getStringValue(key: String, defaultValue: String): String {
        return sharedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    fun setStringValue(key: String, value: String) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putString(key, value) }
    }

    fun getIntValue(key: String, defaultValue: Int): Int {
        return sharedPrefs.getInt(key, defaultValue)
    }

    fun setIntValue(key: String, value: Int) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putInt(key, value) }
    }

    fun getLongValue(key: String, defaultValue: Long): Long {
        return sharedPrefs.getLong(key, defaultValue)
    }

    fun setLongValue(key: String, value: Long) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putLong(key, value) }
    }

    private fun notifyConfigUpdated(key: String, value: Any) {
        EventHandler.fireEventWithScope(Events.Config.GlobalConfigUpdate(key, value))
    }
}