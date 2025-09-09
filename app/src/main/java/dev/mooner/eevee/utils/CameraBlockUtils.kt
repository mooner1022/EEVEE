package dev.mooner.eevee.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import dev.mooner.eevee.service.CameraBlockingAccessibilityService

object CameraBlockUtils {
    
    private val KNOWN_CAMERA_PACKAGES = setOf(
        // Stock Android cameras
        "com.android.camera",
        "com.android.camera2",
        
        // Google Camera
        "com.google.android.GoogleCamera",
        "com.google.android.GoogleCameraEng",
        
        // Manufacturer cameras
        "com.samsung.android.camera",
        "com.sec.android.app.camera",
        "com.huawei.camera",
        "com.xiaomi.camera",
        "com.miui.camera",
        "com.oppo.camera",
        "com.oneplus.camera",
        "com.oneplus.cameravideo",
        "com.vivo.camera",
        "com.motorola.camera",
        "com.motorola.camera2",
        "com.lge.camera",
        "com.htc.camera",
        "com.sony.camera",
        "com.asus.camera",
        "com.lenovo.camera",
        "com.tcl.camera",
        "com.realme.camera",
        
        // Third-party camera apps
        "com.adobe.lrmobile",
        "com.android.camera2.debug",
        "net.sourceforge.opencamera",
        "com.microsoft.office.lync15",
        "com.snapchat.android",
        "com.instagram.android",
        "com.whatsapp",
        "com.facebook.katana"
    )
    
    /**
     * Checks if a package name is likely a camera application
     */
    fun isCameraPackage(packageName: String): Boolean {
        return KNOWN_CAMERA_PACKAGES.contains(packageName) || 
               packageName.contains("camera", ignoreCase = true) ||
               packageName.contains("cam", ignoreCase = true)
    }
    
    /**
     * Gets a list of all known camera packages
     */
    fun getKnownCameraPackages(): Set<String> = KNOWN_CAMERA_PACKAGES
    
    /**
     * Checks if the accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        val serviceName = "${context.packageName}/${CameraBlockingAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }
    
    /**
     * Checks if camera blocking is currently active
     */
    fun isCameraBlockingActive(): Boolean {
        return CameraBlockingAccessibilityService.isServiceEnabled()
    }
    
    /**
     * Opens accessibility settings to enable the service
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Creates an intent to directly open this app's accessibility service settings
     */
    fun createAccessibilityServiceIntent(context: Context): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}