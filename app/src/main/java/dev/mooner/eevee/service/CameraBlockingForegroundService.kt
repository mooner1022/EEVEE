package dev.mooner.eevee.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.mooner.eevee.Constants
import dev.mooner.eevee.R
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.view.MainActivity
import dev.mooner.eevee.view.overlay.OverlayActivity
import dev.mooner.eevee.view.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.contains

class CameraBlockingForegroundService : Service() {

    companion object {
        private const val TAG = "CameraForegroundService"
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "camera_blocking_service"
        private const val CHANNEL_NAME = "카메라 차단 서비스"

        private val LISTENING_KEYS = arrayOf(Constants.KEY_LOCK_STATE, Constants.KEY_LOCK_ACTUALLY)

        private val _ignoreNextEvent = AtomicBoolean(false)
        fun ignoreNextEvent() {
            _ignoreNextEvent.set(true)
        }
        
        @Volatile
        private var serviceInstance: CameraBlockingForegroundService? = null
        
        fun isServiceRunning(): Boolean = serviceInstance != null
        
        fun startService(context: Context) {
            val intent = Intent(context, CameraBlockingForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, CameraBlockingForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationManager: NotificationManager
    private val isNTRIngCamera = AtomicBoolean(false)
    private val isBlockingEnabled = AtomicBoolean(false)
    private var isServiceActive = false

    private var _lifecycleScope: CoroutineScope? = null
    private val lifecycleScope get() = _lifecycleScope!!

    override fun onCreate() {
        super.onCreate()
        _lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        serviceInstance = this
        settingsRepository = SettingsRepository(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        Log.d(TAG, "CameraBlockingForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CameraBlockingForegroundService started")

        updateBlockingState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else
            startForeground(NOTIFICATION_ID, createNotification())
        isServiceActive = true

        lifecycleScope.launch {
            EventHandler.on(this, ::onGlobalConfigUpdated)
        }

        println("onStartCommand")
        setupCameraStateReceiver()
        
        return START_STICKY // Restart service if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
        _lifecycleScope = null
        serviceInstance = null
        isServiceActive = false
        Log.d(TAG, "CameraBlockingForegroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupCameraStateReceiver() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraManager.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                super.onCameraUnavailable(cameraId)
                if (_ignoreNextEvent.get()) {
                    _ignoreNextEvent.set(false)
                    return
                }
                if (!isBlockingEnabled.get())
                    return
                isNTRIngCamera.set(true)
                tryNtrCamera(cameraManager)
            }

            override fun onCameraAvailable(cameraId: String) {
                super.onCameraAvailable(cameraId)
                if (!isNTRIngCamera.get())
                    return
                EventHandler.fireEventWithScope(Events.Camera.OverlayRelease())
                isNTRIngCamera.set(false)
            }
        }, null)
    }

    private fun tryNtrCamera(manager: CameraManager) {
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "카메라 차단 서비스가 백그라운드에서 실행 중입니다"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val status = getServiceStatus()
        val title = if (status.isActive) "카메라 차단 활성화됨" else "카메라 차단 비활성화됨"
        val text = getStatusText(status)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .build()
    }

    private fun updateNotification() {
        if (isServiceActive) {
            val notification = createNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun onGlobalConfigUpdated(event: Events.Config.GlobalConfigUpdate) {
        if (event.key !in LISTENING_KEYS)
            return
        updateBlockingState()
    }

    private fun updateBlockingState() {
        val lockState = settingsRepository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
        val lockActually = settingsRepository.getBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
        isBlockingEnabled.set(lockState && lockActually)
        Log.d("CameraBlockingForegroundService", "Blocking state updated: ${isBlockingEnabled.get()}")
    }

    /*
    private fun monitorSettings() {
        // Update notification periodically to reflect current status
        Thread {
            while (isServiceActive) {
                try {
                    Thread.sleep(5000) // Update every 5 seconds
                    updateNotification()
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }
     */

    private fun getServiceStatus(): ServiceStatus {
        val lockState = settingsRepository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
        val lockActually = settingsRepository.getBooleanValue(Constants.KEY_LOCK_ACTUALLY, true)
        val accessibilityEnabled = CameraBlockingAccessibilityService.isServiceEnabled()
        
        return ServiceStatus(
            isActive = lockState && lockActually && accessibilityEnabled,
            lockState = lockState,
            lockActually = lockActually,
            accessibilityEnabled = accessibilityEnabled
        )
    }

    private fun getStatusText(status: ServiceStatus): String {
        return when {
            status.isActive -> "카메라 앱이 자동으로 차단됩니다"
            !status.lockState -> "기능 잠금이 비활성화되어 있습니다"
            !status.lockActually -> "실제 기능 잠금이 비활성화되어 있습니다"
            !status.accessibilityEnabled -> "접근성 서비스가 비활성화되어 있습니다"
            else -> "서비스 상태를 확인할 수 없습니다"
        }
    }

    private data class ServiceStatus(
        val isActive: Boolean,
        val lockState: Boolean,
        val lockActually: Boolean,
        val accessibilityEnabled: Boolean
    )
}