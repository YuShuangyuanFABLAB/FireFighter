package com.example.firefighterterminal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.firefighterterminal.data.ble.BleManagerRepository
import com.example.firefighterterminal.data.ble.ConnectionState
import com.example.firefighterterminal.data.config.ConfigLoader
import com.example.firefighterterminal.databinding.ActivityMainBinding
import com.example.firefighterterminal.domain.model.IotDevice
import com.example.firefighterterminal.domain.model.MapLayoutConfig
import com.example.firefighterterminal.presentation.ui.device.DeviceListFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MainPagerAdapter
    private var currentTab = 0
    private val configLoader by lazy { ConfigLoader(application) }
    private var mapLayoutConfig: MapLayoutConfig? = null
    private var permissionsGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            BleManagerRepository.initialize(application)
            setupBleCallbacks()
        } else {
            Toast.makeText(this, "蓝牙权限被拒绝，无法扫描设备", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadConfig()
        setupViewPager()
        setupBottomNavigation()

        binding.btnDisconnect.setOnClickListener {
            BleManagerRepository.disconnect()
            switchToTab(0)
        }

        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            permissionsGranted = true
            BleManagerRepository.initialize(application)
            setupBleCallbacks()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun loadConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceConfig = configLoader.loadDeviceConfig()
                mapLayoutConfig = deviceConfig.mapLayout
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "配置加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBleCallbacks() {
        BleManagerRepository.onConnectionStateChanged = { state ->
            runOnUiThread {
                when (state) {
                    is ConnectionState.Connected -> {
                        binding.tvConnectionStatus.text = "已连接"
                        binding.tvConnectionStatus.setTextColor(0xFF4CAF50.toInt())
                        binding.btnDisconnect.visibility = android.view.View.VISIBLE
                    }
                    is ConnectionState.Disconnected -> {
                        binding.tvConnectionStatus.text = "未连接"
                        binding.tvConnectionStatus.setTextColor(0xFFa0a0a0.toInt())
                        binding.btnDisconnect.visibility = android.view.View.GONE
                    }
                    is ConnectionState.Connecting -> {
                        binding.tvConnectionStatus.text = "连接中..."
                        binding.tvConnectionStatus.setTextColor(0xFFFF9800.toInt())
                        binding.btnDisconnect.visibility = android.view.View.GONE
                    }
                    is ConnectionState.Error -> {
                        binding.tvConnectionStatus.text = "连接错误"
                        binding.tvConnectionStatus.setTextColor(0xFFF44336.toInt())
                        binding.btnDisconnect.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    fun connectToDevice(device: IotDevice) {
        val bleConfig = configLoader.loadDeviceConfigSafely()?.bleConfig ?: return
        val serviceUuid = bleConfig.services.firstOrNull()?.let { java.util.UUID.fromString(it.uuid) } ?: return
        val notifyUuid = bleConfig.services.firstOrNull()?.characteristics
            ?.find { it.type == "NOTIFY" }?.let { java.util.UUID.fromString(it.uuid) } ?: return
        val writeUuid = bleConfig.services.firstOrNull()?.characteristics
            ?.find { it.type == "WRITE" }?.let { java.util.UUID.fromString(it.uuid) }
        BleManagerRepository.connectToDevice(device, serviceUuid, notifyUuid, writeUuid)
        switchToTab(1)
    }

    private fun setupViewPager() {
        adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabSelection(position)
            }
        })
        binding.viewPager.post {
            getDeviceListFragment()?.setOnDeviceSelectedListener { device: IotDevice ->
                connectToDevice(device)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.tabDevice.setOnClickListener { switchToTab(0) }
        binding.tabMap.setOnClickListener { switchToTab(1) }
        binding.tabAnalysis.setOnClickListener { switchToTab(2) }
    }

    private fun switchToTab(position: Int) {
        if (currentTab == position) return
        currentTab = position
        binding.viewPager.setCurrentItem(position, false)
        updateTabSelection(position)
    }

    private fun updateTabSelection(position: Int) {
        val defaultColor = 0xFFa0a0a0.toInt()
        val selectedColor = 0xFF4ecdc4.toInt()
        binding.iconDevice.setColorFilter(if (position == 0) selectedColor else defaultColor)
        binding.textDevice.setTextColor(if (position == 0) selectedColor else defaultColor)
        binding.iconMap.setColorFilter(if (position == 1) selectedColor else defaultColor)
        binding.textMap.setTextColor(if (position == 1) selectedColor else defaultColor)
        binding.iconAnalysis.setColorFilter(if (position == 2) selectedColor else defaultColor)
        binding.textAnalysis.setTextColor(if (position == 2) selectedColor else defaultColor)
    }

    fun getDeviceListFragment(): DeviceListFragment? {
        return supportFragmentManager.findFragmentByTag("f${MainPagerAdapter.PAGE_DEVICE}") as? DeviceListFragment
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewPager.adapter = null
        BleManagerRepository.cleanup()
    }
}
