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

    fun appendLogItem(context: Context, item: LogItem): Int {
        val org = readLogData(context).toMutableList()
        val insertionIdx = findInsertPosition(org, item)
        org.add(insertionIdx, item)
        saveLogData(context, org)
        return insertionIdx
    }

    fun saveLogData(context: Context, data: List<LogItem>) {
        val encoded = Json.encodeToString(data)
        getRepo(context).setStringValue(Constants.KEY_LOG_DATA, encoded)
    }

    fun <T : Comparable<T>> findInsertPosition(list: List<T>, item: T): Int {
        var left = 0
        var right = list.size

        while (left < right) {
            val mid = left + (right - left) / 2

            if (list[mid] < item) {
                left = mid + 1
            } else {
                right = mid
            }
        }

        return left
    }

    private fun getRepo(context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}