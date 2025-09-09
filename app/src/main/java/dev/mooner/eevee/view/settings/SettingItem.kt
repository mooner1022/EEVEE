package dev.mooner.eevee.view.settings

import android.view.View

interface SettingItem {
    val type: SettingType
    val key: String
    val title: String
    val summary: String?
    val isEnabled: Boolean
}

data class SettingGroup(
    val title: String,
    val settings: List<SettingItem>
)

data class SwitchSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val defaultValue: Boolean = false
) : SettingItem {
    override val type = SettingType.SWITCH
}

data class ListSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val entries: List<String>,
    val entryValues: List<String>,
    val defaultValue: String
) : SettingItem {
    override val type = SettingType.LIST
}

data class HeaderSetting(
    override val title: String,
    override val summary: String? = null
) : SettingItem {
    override val type = SettingType.HEADER
    override val key = ""
    override val isEnabled = true
}

data class CustomSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val customAction: (view: View) -> Unit
) : SettingItem {
    override val type = SettingType.CUSTOM
}

data class TextSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val defaultValue: String = "",
    val hint: String? = null,
    val inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
) : SettingItem {
    override val type = SettingType.TEXT
}

data class SliderSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val defaultValue: Int = 0,
    val minValue: Int = 0,
    val maxValue: Int = 100,
    val stepSize: Int = 1,
    val unit: String? = null
) : SettingItem {
    override val type = SettingType.SLIDER
}

data class DateTimeSetting(
    override val key: String,
    override val title: String,
    override val summary: String? = null,
    override val isEnabled: Boolean = true,
    val defaultValue: Long = System.currentTimeMillis(),
    val dateFormat: String = "yyyy년 MM월 dd일 HH:mm",
    val use24HourFormat: Boolean = true
) : SettingItem {
    override val type = SettingType.DATE_TIME
}
