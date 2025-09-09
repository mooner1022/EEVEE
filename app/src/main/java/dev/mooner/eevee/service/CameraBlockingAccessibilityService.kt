package dev.mooner.eevee.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.mooner.eevee.Constants
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.utils.CameraBlockUtils
import dev.mooner.eevee.view.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CameraBlockingAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CameraBlockingService"

        private val LISTENING_KEYS = arrayOf(Constants.KEY_LOCK_STATE, Constants.KEY_LOCK_ACTUALLY)
        
        @Volatile
        private var serviceInstance: CameraBlockingAccessibilityService? = null
        
        fun isServiceEnabled(): Boolean = serviceInstance != null
    }

    private lateinit var settingsRepository: SettingsRepository
    private var _lifecycleScope: CoroutineScope? = null
    private val lifecycleScope: CoroutineScope get() = _lifecycleScope!!
    private var isBlockingEnabled = false

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        _lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        settingsRepository = SettingsRepository(this)
        updateBlockingState()

        lifecycleScope.launch {
            EventHandler.on(this, ::onGlobalConfigUpdated)
        }
        
        // Start foreground service to keep the app alive
        CameraBlockingForegroundService.startService(this)
        Log.d(TAG, "CameraBlockingAccessibilityService created and foreground service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        lifecycleScope.cancel()
        _lifecycleScope = null

        // Stop foreground service when accessibility service is destroyed
        CameraBlockingForegroundService.stopService(this)
        Log.d(TAG, "CameraBlockingAccessibilityService destroyed and foreground service stopped")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "CameraBlockingAccessibilityService connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        if (!isBlockingEnabled) {
            return
        }

        val packageName = event.packageName?.toString()
        if (packageName != null && isCameraPackage(packageName)) {
            Log.d(TAG, "Camera app detected: $packageName")
            blockCameraApp()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "CameraBlockingAccessibilityService interrupted")
    }

    private fun onGlobalConfigUpdated(event: Events.Config.GlobalConfigUpdate) {
        if (event.key !in LISTENING_KEYS)
            return
        updateBlockingState()
    }

    private fun updateBlockingState() {
        val lockState = settingsRepository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
        val lockActually = settingsRepository.getBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
        isBlockingEnabled = lockState && lockActually
        Log.d(TAG, "Blocking state updated: $isBlockingEnabled")
    }

    private fun isCameraPackage(packageName: String): Boolean {
        return CameraBlockUtils.isCameraPackage(packageName)
    }

    private fun blockCameraApp() {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "Camera app blocked successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block camera app", e)
            
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                Log.d(TAG, "Navigated to home screen as fallback")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to navigate to home screen", ex)
            }
        }
    }
}