package dev.mooner.eevee.utils

import android.content.Context
import dev.mooner.eevee.Constants
import dev.mooner.eevee.view.log.LogItem
import dev.mooner.eevee.view.settings.SettingsRepository
import kotlinx.serialization.json.Json

object LogUtils {

    fun readLogData(context: Context): List<LogItem> {
        val raw = getRepo(context).getStringValue(Constants.KEY_LOG_DATA)
            ?: return emptyList()
        val parsed = Json.decodeFromString<List<LogItem>>(raw)
        return parsed
    }

    fun appendLogItem(context: Context, item: LogItem) {
        val org = readLogData(context).toMutableList()
        org += item
        saveLogData(context, org)
    }

    fun saveLogData(context: Context, data: List<LogItem>) {
        val encoded = Json.encodeToString(data)
        getRepo(context).setStringValue(Constants.KEY_LOG_DATA, encoded)
    }

    private fun getRepo(context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}