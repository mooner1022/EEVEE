package dev.mooner.eevee.view.log

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dev.mooner.eevee.Constants
import dev.mooner.eevee.R
import dev.mooner.eevee.utils.getAndroidVersionName
import dev.mooner.eevee.view.settings.*

class AddLogDialog(
    private val context: Context,
    private val onLogAdded: (LogItem) -> Unit
) {
    private val viewModel = SettingsViewModel(InMemoryRepository())
    private lateinit var adapter: SettingsAdapter
    
    fun show() {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_settings, null)

        setupDialogContent(view, bottomSheetDialog)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
    
    private fun setupDialogContent(view: View, dialog: BottomSheetDialog) {
        val title = view.findViewById<TextView>(R.id.bottomSheetTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsList)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
        
        title.text = "로그 항목 추가"
        
        // Setup RecyclerView with settings
        adapter = SettingsAdapter(viewModel, context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        // Create settings for the dialog
        val settingGroups = createLogInputSettings()
        adapter.updateSettings(settingGroups)
        
        // Setup buttons
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        confirmButton.setOnClickListener {
            addLogItem()
            dialog.dismiss()
        }
    }
    
    private fun createLogInputSettings(): List<SettingGroup> {
        val logTypeDisplayNames = mapOf(
            LogItem.Type.INITIAL_INSTALL to "최초 설치",
            LogItem.Type.APP_UPDATE to "앱 업데이트", 
            LogItem.Type.TRY_UNLOCK_GPS to "위치기반 해제(GPS) 시도",
            LogItem.Type.TRY_UNLOCK_BEACON to "비콘기반 해제 시도",
            LogItem.Type.UNLOCK to "기능 허용",
            LogItem.Type.LOCK to "기능 차단"
        )
        
        return listOf(
            SettingGroup("로그 정보", listOf(
                ListSetting(
                    key = "log_type",
                    title = "로그 유형",
                    summary = "추가할 로그의 유형을 선택하세요",
                    entries = logTypeDisplayNames.values.toList(),
                    entryValues = logTypeDisplayNames.keys.map { it.name },
                    defaultValue = LogItem.Type.INITIAL_INSTALL.name
                ),
                TextSetting(
                    key = "log_description", 
                    title = "설명",
                    summary = "로그에 대한 추가 설명을 입력하세요 (선택사항)",
                    defaultValue = "",
                    hint = "로그 설명을 입력하세요"
                ),
                DateTimeSetting(
                    key = "log_timestamp",
                    title = "로그 발생 시각",
                    summary = "로그가 발생한 시각을 입력하세요 (선택사항)",
                    defaultValue = System.currentTimeMillis(),
                    dateFormat = "yyyy년 MM월 dd일 HH:mm",
                    use24HourFormat = true
                ),
                TextSetting(
                    key = "app_version",
                    title = "표시될 버전",
                    summary = "로그에 표시될 앱 버전입니다.",
                    defaultValue = "2.1.71",
                    hint = "ex) 2.1.71"
                ),
            ))
        )
    }
    
    private fun addLogItem() {
        val typeString = viewModel.getStringSetting("log_type", LogItem.Type.INITIAL_INSTALL.name)
        val selectedType = LogItem.Type.valueOf(typeString)
        val description = viewModel.getStringSetting("log_description", "")
            .takeIf { it.isNotEmpty() }
        val timestamp = viewModel.getLongSetting("log_timestamp", 0)
            .takeIf { it > 0 }
            ?: System.currentTimeMillis()
        val appVersion = viewModel.getStringSetting("app_version", getAppVersion())

        
        val newLogItem = LogItem(
            type = selectedType,
            desc = description,
            timestamp = timestamp,
            appVersion = appVersion,
            androidVersion = "Android ${getAndroidVersionName()}"
        )
        
        onLogAdded(newLogItem)
    }
    
    private fun getAppVersion(): String {
        return SettingsRepository(context).getStringValue(Constants.KEY_SHOWN_VERSION, "2.1.71")
    }
}