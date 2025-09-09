package dev.mooner.eevee.view.settings

import androidx.core.content.edit

class InMemoryRepository: Repository {
    private val data: MutableMap<String, Any> = hashMapOf()

    override fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defaultValue
    }

    override fun setBooleanValue(key: String, value: Boolean) {
        data[key] = value
    }

    override fun getStringValue(key: String): String? {
        return data[key] as? String
    }

    override fun getStringValue(key: String, defaultValue: String): String {
        return getStringValue(key) ?: defaultValue
    }

    override fun setStringValue(key: String, value: String) {
        data[key] = value
    }

    override fun getIntValue(key: String, defaultValue: Int): Int {
        return data[key] as? Int ?: defaultValue
    }

    override fun setIntValue(key: String, value: Int) {
        data[key] = value
    }

    override fun getLongValue(key: String, defaultValue: Long): Long {
        return data[key] as? Long ?: defaultValue
    }

    override fun setLongValue(key: String, value: Long) {
        data[key] = value
    }
}