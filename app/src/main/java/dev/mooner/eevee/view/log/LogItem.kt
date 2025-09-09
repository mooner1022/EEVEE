package dev.mooner.eevee.view.log

data class LogItem(
    val type: Type,
    val desc: String,
    val timestamp: Long,
    val appVersion: String,
    val androidVersion: String,
) {

    enum class Type {
        APP_UPDATE, UNLOCK_GPS, UNLOCK_BEACON, LOCK
    }
}
