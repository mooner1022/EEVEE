package dev.mooner.eevee.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.toUri
import dev.mooner.eevee.Constants
import dev.mooner.eevee.R
import dev.mooner.eevee.databinding.FragmentDrawerContentBinding
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.event.on
import dev.mooner.eevee.utils.getAndroidVersionCode
import dev.mooner.eevee.utils.getAndroidVersionName
import dev.mooner.eevee.utils.getPhoneBrand
import dev.mooner.eevee.utils.getPhoneManufacturer
import dev.mooner.eevee.utils.getPhoneModel
import dev.mooner.eevee.view.drawer.*
import dev.mooner.eevee.view.drawer.DrawerListAdapter
import dev.mooner.eevee.view.info.InstallInfoActivity
import dev.mooner.eevee.view.log.LogViewActivity
import dev.mooner.eevee.view.settings.SettingsActivity
import dev.mooner.eevee.view.settings.SettingsRepository
import dev.mooner.eevee.view.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DrawerContentFragment : Fragment() {

    companion object {
        const val KEY_ENABLE_VOICE_NOTIFICATION = "enable_voice_noti"
        const val KEY_ENABLE_VIBRATION = "enable_vibration"
    }

    private var _binding: FragmentDrawerContentBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: DrawerListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawerContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventHandler.on(this, ::onLockStateUpdated)
            }
        }
        val locked = SettingsRepository(requireContext()).getBooleanValue(Constants.KEY_LOCK_STATE, false)
        binding.btnDelete.visibility = if (locked) View.GONE else View.VISIBLE
        
        setupDrawerMenu()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        val locked = SettingsRepository(requireContext()).getBooleanValue(Constants.KEY_LOCK_STATE, false)
        binding.btnDelete.visibility = if (locked) View.GONE else View.VISIBLE
    }
    
    private fun setupDrawerMenu() {
        val repo = SettingsRepository(requireContext())
        val drawerItems = listOf(
            HeaderDrawerItem(
                id = 0,
                title = "앱 정보",
            ),
            ArrowDrawerItem(
                id = 1,
                title = "설치정보",
                iconRes = R.drawable.img_icon_settings,
                onItemClick = { _ ->
                    startActivity(Intent(context, InstallInfoActivity::class.java))
                    closeDrawer()
                }
            ),
            ArrowDrawerItem(
                id = 2,
                title = "사용가이드",
                iconRes = R.drawable.img_icon_book,
                onItemClick = { _ ->
                    startActivity(Intent(context, SettingsActivity::class.java))
                    //closeDrawer()
                }
            ),
            ArrowDrawerItem(
                id = 3,
                title = "상세가이드",
                iconRes = R.drawable.img_icon_book,
                onItemClick = { item ->
                    closeDrawer()
                }
            ),
            ArrowDrawerItem(
                id = 4,
                title = "로그보기",
                iconRes = R.drawable.img_icon_list_bullet,
                onItemClick = { item ->
                    startActivity(Intent(context, LogViewActivity::class.java))
                }
            ),
            SwitchDrawerItem(
                id = 5,
                title = "음성알림",
                value = "차단 또는 허용 시 음성 알림",
                iconRes = R.drawable.img_icon_mic,
                switchState = repo.getBooleanValue(KEY_ENABLE_VOICE_NOTIFICATION, true),
                onSwitchToggle = { _, isChecked ->
                    repo.setBooleanValue(KEY_ENABLE_VOICE_NOTIFICATION, isChecked)
                }
            ),
            SwitchDrawerItem(
                id = 6,
                title = "진동알림",
                value = "차단 또는 허용 시 진동 알림",
                iconRes = R.drawable.img_icon_vibration,
                switchState = repo.getBooleanValue(KEY_ENABLE_VIBRATION, true),
                onSwitchToggle = { _, isChecked ->
                    repo.setBooleanValue(KEY_ENABLE_VIBRATION, isChecked)
                }
            ),
            HeaderDrawerItem(
                id = 7,
                title = "시스템 정보"
            ),
            TextDrawerItem(
                id = 8,
                title = "제조사",
                detailText = getPhoneManufacturer(),
                iconRes = R.drawable.img_icon_check,
            ),
            TextDrawerItem(
                id = 9,
                title = "모델명",
                detailText = getPhoneModel(),
                iconRes = R.drawable.img_icon_check,
            ),
            TextDrawerItem(
                id = 10,
                title = "OS버전",
                detailText = getAndroidVersionName(),
                iconRes = R.drawable.img_icon_check,
            ),
            ArrowDrawerItem(
                id = 11,
                title = "App 버전",
                iconRes = R.drawable.img_icon_check,
                value = repo.getStringValue(Constants.KEY_SHOWN_VERSION, "2.1.71"),
                onItemClick = { _ ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("스토어 이동")
                        .setMessage("구글 플레이스토어로 이동합니다.")
                        .setPositiveButton("확인") { dialog, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=kr.go.mnd.mmsa.of".toUri()))
                            dialog.dismiss()
                        }
                        .setNegativeButton("닫기") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    //closeDrawer()
                }
            )
        )
        
        adapter = DrawerListAdapter(requireContext(), drawerItems)
        binding.lvDrawerMenu.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            closeDrawer()
        }
        
        binding.btnDelete.setOnClickListener {
            Toast.makeText(context, "삭제 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun closeDrawer() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(GravityCompat.START)
    }

    private fun onLockStateUpdated(event: Events.MDM.LockStateUpdate) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.btnDelete.visibility = if (event.locked) View.GONE else View.VISIBLE
        }
    }
}