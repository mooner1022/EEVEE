package dev.mooner.eevee

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminBroadcastReceiver: DeviceAdminReceiver() {

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        showToast(context, "Enabled")
        val (dpm, componentName) = requirePolicyManager(context)

        //dpm.setCameraDisabled(componentName, true) // Fuck!!
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val (dpm, componentName) = requirePolicyManager(context)

        dpm.setProfileName(componentName, context.getString(R.string.mdm_devman_profile_name))
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "DISABLE_WARNING"

    override fun onDisabled(context: Context, intent: Intent) =
        showToast(context, "Disabled")

    private fun requirePolicyManager(context: Context): Pair<DevicePolicyManager, ComponentName> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, DeviceAdminBroadcastReceiver::class.java)
        return dpm to componentName
    }
}