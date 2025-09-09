package dev.mooner.eevee.view

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import com.google.android.material.navigation.NavigationView
import com.permissionx.guolindev.PermissionX
import dev.mooner.eevee.Constants
import dev.mooner.eevee.R
import dev.mooner.eevee.UserMode
import dev.mooner.eevee.databinding.ActivityMainBinding
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.service.CameraBlockingManager
import dev.mooner.eevee.view.settings.SettingsActivity
import dev.mooner.eevee.view.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var cameraBlockingManager: CameraBlockingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        val navigationView = binding.navigationDrawer

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            navigationView.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        // Initialize camera blocking manager and auto-start services
        cameraBlockingManager = CameraBlockingManager(this)
        autoStartCameraBlockingServices()

        applyLockState(getIsLocked())

        binding.btnMenu.setOnClickListener {
            //startActivity(Intent(this, SettingsActivity::class.java))
            //return@setOnClickListener
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun getRepository(): SettingsRepository {
        return SettingsRepository(this)
    }

    private fun getCurrentUserMode(): UserMode {
        return getRepository()
            .getStringValue(Constants.KEY_USER_MODE, UserMode.EMPLOYEE.name)
            .let(UserMode::valueOf)
    }

    private fun getIsLocked(): Boolean {
        return getRepository()
            .getBooleanValue(Constants.KEY_LOCK_STATE, false)
    }

    private fun applyUserMode(mode: UserMode) {
        val (bgColorRes, logoImgRes) = when(mode) {
            UserMode.SOLDIER  -> R.color.mdm_locked_soldier  to R.drawable.img_common_user_soldier
            UserMode.EMPLOYEE -> R.color.mdm_locked_employee to R.drawable.img_common_user_employee
            UserMode.VISITOR  -> R.color.mdm_locked_visitor  to R.drawable.img_common_user_vistor // WOW! Typo
            UserMode.GUEST    -> R.color.mdm_locked_guest    to R.drawable.img_common_user_regular_guest
        }

        binding.toolbar.setBackgroundColor(getColor(bgColorRes))
        binding.centerImage.load(logoImgRes)
    }

    private fun applyLockState(locked: Boolean) {
        if (locked) {
            val currentUserMode = getCurrentUserMode()
            applyUserMode(currentUserMode)
            binding.btnNotify.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.GONE
        } else {
            binding.toolbar.setBackgroundColor(getColor(R.color.mdm_unlocked_red))
            binding.btnNotify.visibility = View.GONE
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    private fun onUserModeUpdated(event: Events.MDM.UserModeUpdate) {
        if (!getIsLocked())
            return
        lifecycleScope.launch(Dispatchers.Main) {
            applyUserMode(event.userMode)
        }
    }

    private fun onLockStateUpdated(event: Events.MDM.LockStateUpdate) {
        getRepository().setLongValue(Constants.KEY_LOCK_START_TIME, if (event.locked) System.currentTimeMillis() else 0L)
        lifecycleScope.launch(Dispatchers.Main) {
            applyLockState(event.locked)
        }
        
        // Auto-start or stop camera blocking services when lock state changes
        if (event.locked) {
            autoStartCameraBlockingServices()
        }
    }

    private fun autoStartCameraBlockingServices() {
        try {
            PermissionX.init(this)
                .permissions(Manifest.permission.CAMERA, Manifest.permission.SYSTEM_ALERT_WINDOW)
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(deniedList, "허용좀 해줘요", "OK", "Cancel")
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(deniedList, "설정에서 수동으로 활성화하세요..", "OK", "Cancel")
                }
                .request { allGranted, _, deniedList ->
                    if (!allGranted) {
                        if (Manifest.permission.CAMERA in deniedList) {
                            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW in deniedList) {
                            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                autoStartCameraBlockingServices()
                            }.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
                        }
                        return@request
                    }

                    // Ensure all necessary services are running
                    cameraBlockingManager.ensureServicesRunning()

                    val status = cameraBlockingManager.getBlockingStatus()
                    android.util.Log.d("MainActivity",
                        "Camera blocking auto-start: fullyActive=${status.fullyActive}, " +
                                "accessibilityEnabled=${status.accessibilityServiceEnabled}, " +
                                "accessibilityRunning=${status.accessibilityServiceRunning}, " +
                                "foregroundRunning=${status.foregroundServiceRunning}")

                    // If accessibility service is not enabled, we can't do much more here
                    // The user will need to enable it manually through settings
                    if (!status.accessibilityServiceEnabled) {
                        android.util.Log.w("MainActivity", "Accessibility service not enabled. Services started but blocking won't work until user enables it.")
                    } else if (status.fullyActive) {
                        android.util.Log.i("MainActivity", "Camera blocking is fully active")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to auto-start camera blocking services", e)
        }
    }

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                EventHandler.on(this, ::onUserModeUpdated)
                EventHandler.on(this, ::onLockStateUpdated)
            }
        }
    }
}