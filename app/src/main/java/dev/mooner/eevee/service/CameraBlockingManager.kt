package dev.mooner.eevee.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dev.mooner.eevee.Constants
import dev.mooner.eevee.utils.CameraBlockUtils
import dev.mooner.eevee.view.settings.SettingsRepository

class CameraBlockingManager(private val context: Context) {
    
    private val settingsRepository = SettingsRepository(context)
    
    /**
     * Checks if camera blocking is properly configured and active
     */
    fun isCameraBlockingActive(): Boolean {
        val serviceEnabled = CameraBlockUtils.isAccessibilityServiceEnabled(context)
        val accessibilityServiceRunning = CameraBlockUtils.isCameraBlockingActive()
        val foregroundServiceRunning = CameraBlockingForegroundService.isServiceRunning()
        val lockState = settingsRepository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
        val lockActually = settingsRepository.getBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
        
        return serviceEnabled && accessibilityServiceRunning && foregroundServiceRunning && lockState && lockActually
    }
    
    /**
     * Checks if the accessibility service is enabled in system settings
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return CameraBlockUtils.isAccessibilityServiceEnabled(context)
    }
    
    /**
     * Checks if the accessibility service instance is running
     */
    fun isAccessibilityServiceRunning(): Boolean {
        return CameraBlockUtils.isCameraBlockingActive()
    }
    
    /**
     * Checks if the foreground service is running
     */
    fun isForegroundServiceRunning(): Boolean {
        return CameraBlockingForegroundService.isServiceRunning()
    }
    
    /**
     * Gets the current lock state from settings
     */
    fun isLockEnabled(): Boolean {
        return settingsRepository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
    }
    
    /**
     * Gets the current actual lock state from settings
     */
    fun isActualLockEnabled(): Boolean {
        return settingsRepository.getBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
    }
    
    /**
     * Enables camera lock in settings and starts foreground service if needed
     */
    fun enableCameraLock() {
        settingsRepository.setBooleanValue(Constants.KEY_LOCK_STATE, true)
        settingsRepository.setBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
        
        // Start foreground service if not already running
        if (!CameraBlockingForegroundService.isServiceRunning()) {
            CameraBlockingForegroundService.startService(context)
        }
    }
    
    /**
     * Disables camera lock in settings
     */
    fun disableCameraLock() {
        settingsRepository.setBooleanValue(Constants.KEY_LOCK_STATE, false)
    }
    
    /**
     * Opens accessibility settings for the user to enable the service
     */
    fun openAccessibilitySettings() {
        CameraBlockUtils.openAccessibilitySettings(context)
    }
    
    /**
     * Gets a detailed status of camera blocking components
     */
    fun getBlockingStatus(): CameraBlockingStatus {
        return CameraBlockingStatus(
            accessibilityServiceEnabled = isAccessibilityServiceEnabled(),
            accessibilityServiceRunning = isAccessibilityServiceRunning(),
            foregroundServiceRunning = isForegroundServiceRunning(),
            lockStateEnabled = isLockEnabled(),
            actualLockEnabled = isActualLockEnabled(),
            fullyActive = isCameraBlockingActive()
        )
    }
    
    /**
     * Gets a user-friendly status message
     */
    fun getStatusMessage(): String {
        val status = getBlockingStatus()
        
        return when {
            status.fullyActive -> "카메라 차단이 활성화되어 있습니다."
            !status.accessibilityServiceEnabled -> "접근성 서비스를 활성화해주세요."
            !status.accessibilityServiceRunning -> "접근성 서비스가 실행되지 않았습니다."
            !status.foregroundServiceRunning -> "백그라운드 서비스가 실행되지 않았습니다."
            !status.lockStateEnabled -> "기능 잠금이 비활성화되어 있습니다."
            !status.actualLockEnabled -> "실제 기능 잠금이 비활성화되어 있습니다."
            else -> "알 수 없는 상태입니다."
        }
    }

    /**
     * Attempts to start all necessary services for camera blocking
     */
    fun startAllServices() {
        try {
            // Always start foreground service if camera blocking should be enabled
            if (isLockEnabled() && isActualLockEnabled()) {
                CameraBlockingForegroundService.startService(context)
                android.util.Log.d("CameraBlockingManager", "Foreground service started")
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraBlockingManager", "Failed to start services", e)
        }
    }

    /**
     * Checks if all services should be running based on current settings
     */
    fun shouldServicesBeRunning(): Boolean {
        return isLockEnabled() && isActualLockEnabled()
    }

    /**
     * Performs a health check and starts missing services if needed
     */
    fun ensureServicesRunning() {
        if (shouldServicesBeRunning()) {
            if (!isForegroundServiceRunning()) {
                CameraBlockingForegroundService.startService(context)
                android.util.Log.d("CameraBlockingManager", "Restarted missing foreground service")
            }
        }
    }
}

/**
 * Data class representing the current status of camera blocking
 */
data class CameraBlockingStatus(
    val accessibilityServiceEnabled: Boolean,
    val accessibilityServiceRunning: Boolean,
    val foregroundServiceRunning: Boolean,
    val lockStateEnabled: Boolean,
    val actualLockEnabled: Boolean,
    val fullyActive: Boolean
)