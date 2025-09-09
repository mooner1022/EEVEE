package dev.mooner.eevee.view

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import dev.mooner.eevee.Constants
import dev.mooner.eevee.DeviceAdminBroadcastReceiver
import dev.mooner.eevee.R
import dev.mooner.eevee.UserMode
import dev.mooner.eevee.databinding.FragmentMainContentBinding
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.utils.LogUtils
import dev.mooner.eevee.utils.VibrationUtils
import dev.mooner.eevee.view.gps.UnlockGPSActivity
import dev.mooner.eevee.view.log.LogItem
import dev.mooner.eevee.view.settings.SettingsRepository
import kotlinx.coroutines.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

class MainContentFragment : Fragment() {

    private var _binding: FragmentMainContentBinding? = null
    private val binding get() = _binding!!

    private var _repository: SettingsRepository? = null
    private val repository get() = _repository!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _repository = SettingsRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val currentUserMode = getCurrentUserMode()
        //applyUserMode(currentUserMode)
        applyLockState(getIsLocked())

        binding.tvAgentVersion.text = repository.getStringValue(Constants.KEY_SHOWN_VERSION, "2.1.71")
        binding.tvInstallDate.text = repository.getLongValue(Constants.KEY_INSTALL_TIME, System.currentTimeMillis())
            .let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
            }

        binding.btnCameraDeny.setOnClickListener {
            repository.setBooleanValue(Constants.KEY_LOCK_STATE, true)
            EventHandler.fireEventWithScope(Events.MDM.LockStateUpdate(locked = true))
            VibrationUtils.vibrate(requireContext(), Constants.UNLOCK_VIB_DURATION)
            LogUtils.appendLogItem(requireContext(), LogItem(
                type = LogItem.Type.LOCK,
                appVersion = repository.getStringValue(Constants.KEY_SHOWN_VERSION, "2.1.71"),
            ))

            val devmanName = ComponentName(requireContext(), DeviceAdminBroadcastReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, devmanName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "EXPL..")
            }
            startActivity(intent)
        }

        binding.btnAllowGPS.setOnClickListener {
            startActivity(Intent(requireContext(), UnlockGPSActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _repository = null
    }

    private fun getCurrentUserMode(): UserMode {
        return repository
            .getStringValue(Constants.KEY_USER_MODE, UserMode.EMPLOYEE.name)
            .let(UserMode::valueOf)
    }

    private fun getIsLocked(): Boolean {
        return repository.getBooleanValue(Constants.KEY_LOCK_STATE, false)
    }

    private fun applyUserMode(mode: UserMode) {
        val (bgColorRes, bgWaveImgRes) = when(mode) {
            UserMode.SOLDIER  -> R.color.mdm_locked_soldier  to R.drawable.img_bg_user_soldier_sub
            UserMode.EMPLOYEE -> R.color.mdm_locked_employee to R.drawable.img_bg_user_visitor_sub
            UserMode.VISITOR  -> R.color.mdm_locked_visitor  to R.drawable.img_bg_user_visitor_sub
            UserMode.GUEST    -> R.color.mdm_locked_guest    to R.drawable.img_bg_user_regular_guest_sub
        }
        binding.rlTitle.setBackgroundColor(requireContext().getColor(bgColorRes))
        binding.rlSubTitle.setBackgroundResource(bgWaveImgRes)
    }

    private fun applyLockState(locked: Boolean) {
        if (locked) {
            val currentUserMode = getCurrentUserMode()
            applyUserMode(currentUserMode)
            binding.pdProgress.visibility = View.VISIBLE

            binding.ivCameraSticker.visibility = View.INVISIBLE
            binding.ivCameraDenySticker.visibility = View.VISIBLE
            binding.llDelayTime.visibility = View.VISIBLE

            binding.llButton.visibility = View.VISIBLE
            binding.btnCameraDeny.visibility = View.GONE

            binding.ivCamera.load(R.drawable.img_policy_camera_on)
            binding.llCheckInTime.visibility = View.VISIBLE

            repository.getLongValue(Constants.KEY_LOCK_START_TIME, System.currentTimeMillis())
                .let {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                }
                .let(binding.tvCheckInTime::setText)

            updateLockDurationText()
        } else {
            binding.rlTitle.setBackgroundColor(requireContext().getColor(R.color.mdm_unlocked_red))
            binding.rlSubTitle.setBackgroundResource(R.drawable.img_bg_user_out_sub)
            binding.pdProgress.visibility = View.GONE

            binding.ivCameraSticker.visibility = View.VISIBLE
            binding.ivCameraDenySticker.visibility = View.INVISIBLE
            binding.llDelayTime.visibility = View.GONE

            binding.llButton.visibility = View.GONE
            binding.btnCameraDeny.visibility = View.VISIBLE

            binding.ivCamera.load(R.drawable.img_policy_camera_off)
            binding.llCheckInTime.visibility = View.GONE
        }
    }

    private fun updateLockDurationText() {
        val lockStartMillis = repository.getLongValue(Constants.KEY_LOCK_START_TIME, System.currentTimeMillis())
        val startDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(lockStartMillis),
            ZoneId.systemDefault()
        )
        val nowDate = LocalDateTime.now(ZoneId.systemDefault())

        val (diffDate, diffHour, diffMinute, diffSeconds) = run {
            arrayOf(
                ChronoUnit.DAYS.between(startDate, nowDate),
                ChronoUnit.HOURS.between(startDate, nowDate) % 24,
                ChronoUnit.MINUTES.between(startDate, nowDate) % 60,
                ChronoUnit.SECONDS.between(startDate, nowDate) % 60,
            )
        }
        binding.tvDay.text = diffDate.toString()
        binding.tvHour.text = diffHour.toString().padStart(2, '0')
        binding.tvMin.text = diffMinute.toString().padStart(2, '0')
        binding.tvSec.text = diffSeconds.toString().padStart(2, '0')
    }

    private fun onUserModeUpdated(event: Events.MDM.UserModeUpdate) {
        if (!getIsLocked())
            return
        lifecycleScope.launch(Dispatchers.Main) {
            applyUserMode(event.userMode)
        }
    }

    private fun onLockStateUpdated(event: Events.MDM.LockStateUpdate) {
        lifecycleScope.launch(Dispatchers.Main) {
            applyLockState(event.locked)
        }
    }

    private suspend fun onGlobalConfigUpdated(event: Events.Config.GlobalConfigUpdate) {
        val key = event.key
        val value = event.value

        if (key == Constants.KEY_INSTALL_TIME || key == Constants.KEY_LOCK_START_TIME) {
            withContext(Dispatchers.Main) {
                (if (key == Constants.KEY_INSTALL_TIME) binding.tvInstallDate else binding.tvCheckInTime).text =
                    (value as Long).let {
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                    }
            }
        }
    }

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                EventHandler.on(this, ::onUserModeUpdated)
                EventHandler.on(this, ::onLockStateUpdated)
                EventHandler.on(this, ::onGlobalConfigUpdated)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    withContext(Dispatchers.Main) {
                        updateLockDurationText()
                    }
                    delay(1.seconds)
                }
            }
        }
    }
}