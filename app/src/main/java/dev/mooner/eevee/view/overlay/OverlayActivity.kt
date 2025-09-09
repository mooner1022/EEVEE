package dev.mooner.eevee.view.overlay

import android.content.Intent
import android.graphics.Color
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.service.CameraBlockingForegroundService
import kotlinx.coroutines.launch

class OverlayActivity : AppCompatActivity() {

    private var camera: CameraDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(createTransparentLayout())

        // 카메라 초기화
        CameraBlockingForegroundService.ignoreNextEvent()
        initCamera()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                EventHandler.on(this, ::onOverlayReleased)
            }
        }

        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    override fun onUserLeaveHint() {
        // 홈키를 눌렀을 때 액티비티를 백그라운드로 보내지만 종료하지 않음
        // moveTaskToBack(true) // 필요시 사용
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera()
    }

    private fun createTransparentLayout(): View {
        return LinearLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun initCamera() {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            tryNtrCamera(cameraManager)
        } catch (e: Exception) {
            Log.e("CameraOverlay", "카메라 초기화 실패", e)
        }
    }

    private fun tryNtrCamera(manager: CameraManager) {
        val cameraId = try {
            manager.cameraIdList[0] ?: "0"
        } catch (_: Exception) { "0" }

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Toast.makeText(this@OverlayActivity, "보안정책에 따라 카메라를 사용할 수 없어요.", Toast.LENGTH_SHORT).show()
                Log.d("CameraOverlay", "Blocked camera access")
                this@OverlayActivity.camera = camera
            }

            override fun onDisconnected(camera: CameraDevice) {}

            override fun onError(camera: CameraDevice, error: Int) {}
        }, null)
    }

    private fun releaseCamera() {
        camera?.close()
        camera = null
    }

    private fun onOverlayReleased(event: Events.Camera.OverlayRelease) {
        finish()
    }
}