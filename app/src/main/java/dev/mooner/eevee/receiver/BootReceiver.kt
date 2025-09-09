package dev.mooner.eevee.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.mooner.eevee.Constants
import dev.mooner.eevee.service.CameraBlockingForegroundService
import dev.mooner.eevee.service.CameraBlockingManager
import dev.mooner.eevee.utils.CameraBlockUtils
import dev.mooner.eevee.view.settings.SettingsRepository

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot completed or package updated, checking camera blocking settings")
                handleBootOrUpdate(context)
            }
        }
    }

    private fun handleBootOrUpdate(context: Context) {
        val cameraBlockingManager = CameraBlockingManager(context)
        
        Log.d(TAG, "Handling boot/update event, checking camera blocking settings")
        
        if (cameraBlockingManager.shouldServicesBeRunning()) {
            // Start all necessary services
            startCameraBlockingServices(context, cameraBlockingManager)
        } else {
            Log.d(TAG, "Camera blocking disabled in settings, not starting services")
        }
    }

    private fun startCameraBlockingServices(context: Context, cameraBlockingManager: CameraBlockingManager) {
        try {
            // Use manager to start all services
            cameraBlockingManager.startAllServices()
            
            val status = cameraBlockingManager.getBlockingStatus()
            Log.d(TAG, "Services started on boot: " +
                "foregroundRunning=${status.foregroundServiceRunning}, " +
                "accessibilityEnabled=${status.accessibilityServiceEnabled}")
            
            if (!status.accessibilityServiceEnabled) {
                Log.w(TAG, "Accessibility service is not enabled. User needs to enable it manually.")
                // The foreground service will show appropriate status in notification
            } else if (status.fullyActive) {
                Log.i(TAG, "Camera blocking is fully active after boot")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera blocking services on boot", e)
        }
    }
}