package dev.mooner.eevee.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.mooner.eevee.view.DrawerContentFragment.Companion.KEY_ENABLE_VIBRATION
import dev.mooner.eevee.view.settings.SettingsRepository

object VibrationUtils {

    fun vibrate(context: Context, duration: Long) {
        if (!SettingsRepository(context).getBooleanValue(KEY_ENABLE_VIBRATION, true))
            return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}