package dev.mooner.eevee.view.log

import kotlinx.serialization.Serializable

@Serializable
data class LogItem(
    val type: Type,
    val desc: String? = null,
    val timestamp: Long,
    val appVersion: String,
    val androidVersion: String,
): Comparable<LogItem> {

    enum class Type {
        INITIAL_INSTALL, APP_UPDATE, TRY_UNLOCK_GPS, TRY_UNLOCK_BEACON, UNLOCK, LOCK
    }

    override fun compareTo(other: LogItem): Int {
        return other.timestamp.compareTo(this.timestamp)
    }
}
