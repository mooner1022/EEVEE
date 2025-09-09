package dev.mooner.eevee.view.log

import dev.mooner.eevee.utils.getAndroidVersionName
import kotlinx.serialization.Serializable

@Serializable
data class LogItem(
    val type: Type,
    val desc: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String,
    val androidVersion: String = "Android ${getAndroidVersionName()}",
): Comparable<LogItem> {

    enum class Type {
        INITIAL_INSTALL, APP_UPDATE, TRY_UNLOCK_GPS, TRY_UNLOCK_BEACON, UNLOCK, LOCK
    }

    override fun compareTo(other: LogItem): Int {
        return other.timestamp.compareTo(this.timestamp)
    }
}
