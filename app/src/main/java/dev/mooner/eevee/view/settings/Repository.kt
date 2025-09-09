package dev.mooner.eevee.view.settings

import androidx.core.content.edit

interface Repository {

    fun getBooleanValue(key: String, defaultValue: Boolean): Boolean

    fun setBooleanValue(key: String, value: Boolean)

    fun getStringValue(key: String): String?

    fun getStringValue(key: String, defaultValue: String): String

    fun setStringValue(key: String, value: String)

    fun getIntValue(key: String, defaultValue: Int): Int

    fun setIntValue(key: String, value: Int)

    fun getLongValue(key: String, defaultValue: Long): Long

    fun setLongValue(key: String, value: Long)
}