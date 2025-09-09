package dev.mooner.eevee.view.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.mooner.eevee.Constants
import dev.mooner.eevee.R
import dev.mooner.eevee.UserMode
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var adapter: SettingsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "설정"
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.settingsRecyclerView)
        adapter = SettingsAdapter(viewModel, this)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
            addItemDecoration(DividerItemDecoration(this@SettingsActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(this)
        viewModel = ViewModelProvider(this, SettingsViewModelFactory(repository))[SettingsViewModel::class.java]
        viewModel.initializeWithContext(this)
    }

    private fun observeViewModel() {
        viewModel.settingsData.observe(this) { settingGroups ->
            adapter.updateSettings(settingGroups)
        }

        viewModel.settingChanged.observe(this) { (key, value) ->
            // 설정 변경 처리 (예: 테마 변경 시 즉시 적용)
            handleSettingChange(key, value)
        }
    }

    private fun handleSettingChange(key: String, value: Any) {
        when (key) {
            "theme" -> {
                // 테마 변경 처리
                applyTheme(value as String)
            }
            "notifications_enabled" -> {
                // 알림 설정 변경 처리
                toggleNotifications(value as Boolean)
            }
            Constants.KEY_USER_MODE -> {
                val mode = UserMode.valueOf(value as String)
                applyUserMode(mode)
            }
            Constants.KEY_LOCK_STATE -> {
                val locked = value as Boolean
                applyLockState(locked)
            }
        }
        //EventHandler.fireEventWithScope(Events.Config.GlobalConfigUpdate(key, value))
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun applyUserMode(mode: UserMode) {
        EventHandler.fireEventWithScope(Events.MDM.UserModeUpdate(
            userMode = mode,
        ))
    }

    private fun applyLockState(locked: Boolean) {
        EventHandler.fireEventWithScope(Events.MDM.LockStateUpdate(
            locked = locked,
        ))
    }

    private fun toggleNotifications(enabled: Boolean) {
        // 알림 토글 처리
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}