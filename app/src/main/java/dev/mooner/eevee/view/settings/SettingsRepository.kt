package dev.mooner.eevee.view.settings

import android.content.Context
import androidx.core.content.edit
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events

class SettingsRepository(context: Context): Repository {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        return sharedPrefs.getBoolean(key, defaultValue)
    }

    override fun setBooleanValue(key: String, value: Boolean) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putBoolean(key, value) }
    }

    override fun getStringValue(key: String): String? {
        return sharedPrefs.getString(key, null)
    }

    override fun getStringValue(key: String, defaultValue: String): String {
        return getStringValue(key) ?: defaultValue
    }

    override fun setStringValue(key: String, value: String) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putString(key, value) }
    }

    override fun getIntValue(key: String, defaultValue: Int): Int {
        return sharedPrefs.getInt(key, defaultValue)
    }

    override fun setIntValue(key: String, value: Int) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putInt(key, value) }
    }

    override fun getLongValue(key: String, defaultValue: Long): Long {
        return sharedPrefs.getLong(key, defaultValue)
    }

    override fun setLongValue(key: String, value: Long) {
        notifyConfigUpdated(key, value)
        sharedPrefs.edit { putLong(key, value) }
    }

    private fun notifyConfigUpdated(key: String, value: Any) {
        EventHandler.fireEventWithScope(Events.Config.GlobalConfigUpdate(key, value))
    }
}