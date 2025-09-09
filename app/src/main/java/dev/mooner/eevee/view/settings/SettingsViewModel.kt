package dev.mooner.eevee.view.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.mooner.eevee.Constants
import dev.mooner.eevee.UserMode
import dev.mooner.eevee.service.CameraBlockingManager
import dev.mooner.eevee.view.info.AppInfoActivity

class SettingsViewModel(private val repository: Repository = InMemoryRepository()) : ViewModel() {

    private val _settingsData = MutableLiveData<List<SettingGroup>>()
    val settingsData: LiveData<List<SettingGroup>> = _settingsData

    private val _settingChanged = MutableLiveData<Pair<String, Any>>()
    val settingChanged: LiveData<Pair<String, Any>> = _settingChanged

    private lateinit var cameraBlockingManager: CameraBlockingManager

    fun initializeWithContext(context: android.content.Context) {
        cameraBlockingManager = CameraBlockingManager(context)
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val settings = listOf(
            SettingGroup("기능 설정", listOf(
                SwitchSetting(
                    key = Constants.KEY_LOCK_STATE,
                    title = "기능 잠금 설정",
                    summary = "현재 기능 잠금 상태를 설정합니다.",
                    defaultValue = true,
                ),
                ListSetting(
                    key = Constants.KEY_USER_MODE,
                    title = "사용자 모드",
                    summary = "사용할 사용자 모드를 선택합니다.",
                    entries = UserMode.entries.map { it.krName },
                    entryValues = UserMode.entries.map { it.name },
                    defaultValue = UserMode.EMPLOYEE.name
                ),
            )),
            SettingGroup("일반 설정", listOf(
                SwitchSetting(
                    key = Constants.KEY_LOCK_ACTUALLY,
                    title = "실제 기능 잠금",
                    summary = "카메라 기능을 실제로 제한합니다.",
                    defaultValue = true
                ),
                SwitchSetting(
                    key = Constants.KEY_EDIT_LOG,
                    title = "로그 편집 활성화",
                    summary = "로그 편집을 가능하게 합니다.\n스와이프하여 항목을 제거할 수 있습니다.",
                    defaultValue = false
                ),
                TextSetting(
                    key = Constants.KEY_SHOWN_VERSION,
                    title = "표시될 버전",
                    summary = "앱에 표시될 버전입니다.",
                    defaultValue = "2.1.71",
                    hint = "ex) 2.1.71"
                ),
                DateTimeSetting(
                    key = Constants.KEY_INSTALL_TIME,
                    title = "앱 설치 시각",
                    summary = "앱을 설치한 시각을 수동으로 설정합니다.",
                    defaultValue = System.currentTimeMillis(),
                    dateFormat = "yyyy년 MM월 dd일 HH:mm",
                    use24HourFormat = true
                ),
                DateTimeSetting(
                    key = Constants.KEY_LOCK_START_TIME,
                    title = "기능 잠금 시작 시각",
                    summary = "기능 잠금을 시작한 시각을 수동으로 설정합니다.",
                    defaultValue = System.currentTimeMillis(),
                    dateFormat = "yyyy년 MM월 dd일 HH:mm",
                    use24HourFormat = true
                ),
                ListSetting(
                    key = "theme",
                    title = "테마",
                    summary = "앱 테마를 선택합니다.",
                    entries = listOf("시스템 기본값", "라이트", "다크"),
                    entryValues = listOf("system", "light", "dark"),
                    defaultValue = "system"
                ),
                /*
                SliderSetting(
                    key = "cache_size",
                    title = "캐시 크기",
                    summary = "앱이 사용할 캐시 크기를 설정합니다",
                    defaultValue = 100,
                    minValue = 50,
                    maxValue = 500,
                    stepSize = 10,
                    unit = "MB"
                ),
                 */
            )),
            SettingGroup("고급 설정", listOf(
                CustomSetting(
                    key = "camera_blocking_setup",
                    title = "카메라 차단 서비스 설정",
                    summary = "접근성 서비스를 통한 카메라 차단 기능을 설정합니다",
                    customAction = { setupCameraBlocking() }
                ),
                CustomSetting(
                    key = "about",
                    title = "앱 정보",
                    summary = "버전 정보 및 라이선스",
                    customAction = { showAbout(it.context) }
                )
            ))
        )
        _settingsData.value = settings
    }

    fun updateBooleanSetting(key: String, value: Boolean) {
        repository.setBooleanValue(key, value)
        _settingChanged.value = Pair(key, value)
    }

    fun updateStringSetting(key: String, value: String) {
        repository.setStringValue(key, value)
        _settingChanged.value = Pair(key, value)
    }

    fun updateIntSetting(key: String, value: Int) {
        repository.setIntValue(key, value)
        _settingChanged.value = Pair(key, value)
    }

    fun updateLongSetting(key: String, value: Long) {
        repository.setLongValue(key, value)
        _settingChanged.value = Pair(key, value)
    }

    fun getBooleanSetting(key: String, defaultValue: Boolean): Boolean {
        return repository.getBooleanValue(key, defaultValue)
    }

    fun getStringSetting(key: String, defaultValue: String): String {
        return repository.getStringValue(key, defaultValue)
    }

    fun getIntSetting(key: String, defaultValue: Int): Int {
        return repository.getIntValue(key, defaultValue)
    }

    fun getLongSetting(key: String, defaultValue: Long): Long {
        return repository.getLongValue(key, defaultValue)
    }

    private fun setupCameraBlocking() {
        if (::cameraBlockingManager.isInitialized) {
            cameraBlockingManager.openAccessibilitySettings()
        }
    }

    private fun showAbout(context: Context) {
        context.startActivity(Intent(context, AppInfoActivity::class.java))
    }

    fun getCameraBlockingStatus(): String {
        return if (::cameraBlockingManager.isInitialized) {
            cameraBlockingManager.getStatusMessage()
        } else {
            "카메라 차단 관리자가 초기화되지 않았습니다."
        }
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}