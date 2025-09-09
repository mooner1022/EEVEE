package dev.mooner.eevee.view.gps

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.mooner.eevee.Constants
import dev.mooner.eevee.DeviceAdminBroadcastReceiver
import dev.mooner.eevee.LocationData
import dev.mooner.eevee.LocationWithDistance
import dev.mooner.eevee.databinding.ActivityUnlockGpsBinding
import dev.mooner.eevee.event.EventHandler
import dev.mooner.eevee.event.Events
import dev.mooner.eevee.utils.AssetUtils
import dev.mooner.eevee.utils.VibrationUtils
import dev.mooner.eevee.view.settings.SettingsRepository
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class UnlockGPSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockGpsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: LocationListAdapter

    private val locationList: List<LocationData> by lazy {
        AssetUtils.loadGPSLocation(this)
    }
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            binding.pbLocation.visibility = View.GONE
            binding.tvDescriptionSub.visibility = View.VISIBLE
            Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUnlockGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        adapter = LocationListAdapter(this, emptyList())
        binding.lvResult.adapter = adapter
        binding.ibBack.setOnClickListener {
            finish()
        }
        binding.btnRequest.setOnClickListener {
            binding.tvDescriptionSub.visibility = View.GONE
            binding.pbLocation.visibility = View.VISIBLE
            
            checkLocationPermissionAndGetLocation()
        }
    }
    
    private fun checkLocationPermissionAndGetLocation() {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED || 
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            requestLocationPermissions()
        }
    }
    
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            binding.pbLocation.visibility = View.GONE
            binding.tvDescriptionSub.visibility = View.VISIBLE
            Toast.makeText(this, "GPS가 비활성화되어 있습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                binding.pbLocation.visibility = View.GONE
                
                if (location != null) {
                    val longitude = location.longitude
                    val latitude = location.latitude
                    
                    showLocationResult(latitude, longitude)
                } else {
                    binding.tvDescriptionSub.visibility = View.VISIBLE
                    Toast.makeText(this, "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                binding.pbLocation.visibility = View.GONE
                binding.tvDescriptionSub.visibility = View.VISIBLE
                Toast.makeText(this, "위치 조회 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showLocationResult(latitude: Double, longitude: Double) {
        val locations = findKNearestWithDistance(
            target = LocationData("", latitude, longitude, 0),
            points = locationList,
            k = 20,
        )
        adapter.updateItems(locations)

        val nearest = locations.first()
        if (nearest.distance.toInt() > nearest.location.radius + 5000)
            return

        val repository = SettingsRepository(this)
        repository.setBooleanValue(Constants.KEY_LOCK_STATE, false)
        EventHandler.fireEventWithScope(Events.MDM.LockStateUpdate(locked = false))
        VibrationUtils.vibrate(this, Constants.UNLOCK_VIB_DURATION)
        AlertDialog.Builder(this@UnlockGPSActivity)
            .setMessage("""
                |기능을 허용합니다.
                |허용 가능 위치 확인 : ${nearest.location.name}
                """.trimMargin())
            .setPositiveButton("확인") { _, _ ->
                val context = this@UnlockGPSActivity
                val dpm = context.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val devmanName = ComponentName(context, DeviceAdminBroadcastReceiver::class.java)
                if (dpm.isAdminActive(devmanName))
                    dpm.removeActiveAdmin(devmanName)
                else
                    Toast.makeText(context, "설치된 관리자 프로필 없음", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun findKNearestWithDistance(target: LocationData, points: List<LocationData>, k: Int): List<LocationWithDistance> {
        if (k <= 0 || points.isEmpty()) return emptyList()

        if (k >= points.size) {
            return points
                .map { LocationWithDistance(it, calculateDistanceInMeters(target, it)) }
                .sortedBy { it.distance }
        }

        val maxHeap = PriorityQueue<LocationWithDistance>()

        for (point in points) {
            val distanceSquared = calculateDistanceSquared(target, point)

            when {
                maxHeap.size < k -> {
                    maxHeap.offer(LocationWithDistance(point, distanceSquared))
                }
                distanceSquared < maxHeap.peek()!!.distance -> {
                    maxHeap.poll()
                    maxHeap.offer(LocationWithDistance(point, distanceSquared))
                }
            }
        }

        return maxHeap.map { locationWithSquaredDistance ->
            LocationWithDistance(
                locationWithSquaredDistance.location,
                calculateDistanceInMeters(target, locationWithSquaredDistance.location)
            )
        }.sortedBy { it.distance }
    }

    private fun calculateDistance(p1: LocationData, p2: LocationData): Double {
        val dx = p1.latitude - p2.latitude
        val dy = p1.longitude - p2.longitude
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateDistanceSquared(p1: LocationData, p2: LocationData): Double {
        val dx = p1.latitude - p2.latitude
        val dy = p1.longitude - p2.longitude
        return dx * dx + dy * dy
    }

    private fun calculateDistanceInMeters(p1: LocationData, p2: LocationData): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLatRad = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLonRad = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}