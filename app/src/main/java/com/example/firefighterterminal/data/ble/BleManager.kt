package com.example.firefighterterminal.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firefighterterminal.domain.model.IotDevice
import java.util.UUID

/**
 * BLE管理器
 * 负责BLE设备的扫描、连接、断开等操作
 */
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"
        private const val SCAN_TIMEOUT_MS = 10000L
        private const val MTU_FALLBACK_DELAY_MS = 2000L
        private const val CCCD_RETRY_DELAY_MS = 500L
        private const val CCCD_MAX_ATTEMPTS = 3
        private const val READY_COMMAND = "READY"
    }

    // Bluetooth相关
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    // 扫描相关
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val scanHandler = Handler(Looper.getMainLooper())

    // 连接相关
    private var bluetoothGatt: BluetoothGatt? = null
    private val connectionHandler = Handler(Looper.getMainLooper())

    // 通知设置状态 (每次连接生命周期内有效)
    private var notificationSetupDone = false
    private var cccdWriteAttempts = 0
    private var currentNotifyUuid: UUID? = null
    private var currentWriteUuid: UUID? = null
    private val mtuFallbackRunnable = Runnable {
        Log.w(TAG, "onMtuChanged 超时未回调，使用默认 MTU 继续启用通知")
        proceedToEnableNotifications()
    }

    // 状态 - 使用MutableLiveData以兼容ViewModel
    private val _scanState = MutableLiveData<ScanState>()
    val scanState: LiveData<ScanState> = _scanState

    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableLiveData<List<IotDevice>>()
    val discoveredDevices: LiveData<List<IotDevice>> = _discoveredDevices

    // 回调
    var onDataReceived: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null

    // 写入队列 - 用于处理连续写入
    private data class PendingWrite(
        val characteristicUuid: UUID,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PendingWrite
            return characteristicUuid == other.characteristicUuid && data.contentEquals(other.data)
        }
        override fun hashCode(): Int {
            return 31 * characteristicUuid.hashCode() + data.contentHashCode()
        }
    }
    private val writeQueue = ArrayDeque<PendingWrite>()
    private var isWriting = false

    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter?.isEnabled == true
    }

    /**
     * 检查是否有必要的权限
     */
    fun hasRequiredPermissions(): Boolean {
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

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 开始扫描设备
     * @param deviceFilter 设备名称过滤器，默认只扫描ESP32设备
     * @param timeoutMs 扫描超时时间，默认10秒
     */
    @SuppressLint("MissingPermission")
    fun startScan(deviceFilter: String? = "ESP32", timeoutMs: Long = SCAN_TIMEOUT_MS) {
        if (!isBluetoothAvailable()) {
            Log.w(TAG, "蓝牙不可用")
            return
        }

        if (!hasRequiredPermissions()) {
            Log.w(TAG, "缺少必要权限")
            return
        }

        // 停止当前扫描（如果有）
        stopScan()

        bluetoothScanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            Log.w(TAG, "无法获取BLE扫描器")
            return
        }

        _scanState.postValue(ScanState.Scanning)
        _discoveredDevices.postValue(emptyList())

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name ?: "未知设备"
                val rssi = result.rssi

                // 应用过滤器
                val matchFilter = deviceFilter?.let { pattern ->
                    deviceName.contains(pattern, ignoreCase = true)
                } ?: true

                if (matchFilter) {
                    val iotDevice = IotDevice(
                        name = deviceName,
                        address = device.address,
                        rssi = rssi,
                        rawDevice = device
                    )

                    // 避免重复添加
                    val currentList = _discoveredDevices.value?.toMutableList() ?: mutableListOf()
                    if (currentList.none { it.address == iotDevice.address }) {
                        currentList.add(iotDevice)
                        _discoveredDevices.postValue(currentList)
                        Log.d(TAG, "发现设备: $deviceName (${device.address}), RSSI: $rssi")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "扫描失败，错误代码: $errorCode")
                _scanState.postValue(ScanState.Error(errorCode.toString()))
            }
        }

        bluetoothScanner?.startScan(scanCallback)

        // 设置扫描超时
        scanHandler.postDelayed({
            stopScan()
        }, timeoutMs)
    }

    /**
     * 停止扫描
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let {
            bluetoothScanner?.stopScan(it)
        }
        scanCallback = null
        _scanState.postValue(ScanState.Idle)
        scanHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 连接到设备
     * @param device 要连接的IoT设备
     * @param characteristicUuid 通知特征值UUID (接收数据)
     * @param writeUuid 写入特征值UUID (发送命令/READY握手，可为null)
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: IotDevice, serviceUuid: UUID, characteristicUuid: UUID, writeUuid: UUID? = null) {
        // 停止扫描
        stopScan()

        // 只在已连接时才断开（避免触发不必要的断开状态）
        if (bluetoothGatt != null) {
            disconnectQuietly()
        }

        // 重置通知设置状态并记录本次连接的特征值
        resetNotificationSetup()
        currentNotifyUuid = characteristicUuid
        currentWriteUuid = writeUuid

        _connectionState.postValue(ConnectionState.Connecting(device))

        try {
            val rawDevice = device.rawDevice
            if (rawDevice != null) {
                bluetoothGatt = rawDevice.connectGatt(context, false, createGattCallback())
                Log.d(TAG, "正在连接到设备: ${device.name}")
            } else {
                _connectionState.postValue(ConnectionState.Error("设备信息无效"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}")
            _connectionState.postValue(ConnectionState.Error(e.message ?: "未知错误"))
        }
    }

    /**
     * 重置通知设置状态 (连接建立前/断开时调用)
     */
    private fun resetNotificationSetup() {
        notificationSetupDone = false
        cccdWriteAttempts = 0
        connectionHandler.removeCallbacks(mtuFallbackRunnable)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        // 清理写入队列
        writeQueue.clear()
        isWriting = false

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null

        resetNotificationSetup()

        val disconnectedState = ConnectionState.Disconnected
        _connectionState.postValue(disconnectedState)
        onConnectionStateChanged?.invoke(disconnectedState)  // 触发回调

        connectionHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 静默断开连接（不触发状态变化）
     * 用于连接新设备前清理旧连接
     */
    @SuppressLint("MissingPermission")
    private fun disconnectQuietly() {
        // 清理写入队列
        writeQueue.clear()
        isWriting = false

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null

        resetNotificationSetup()
        connectionHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 启用通知（MTU 协商完成或兜底超时后调用，每次连接只执行一次）
     */
    @SuppressLint("MissingPermission")
    private fun proceedToEnableNotifications() {
        if (notificationSetupDone) return
        notificationSetupDone = true

        val uuid = currentNotifyUuid ?: run {
            Log.e(TAG, "proceedToEnableNotifications: 未记录通知特征值UUID")
            return
        }
        val gatt = bluetoothGatt ?: run {
            Log.e(TAG, "proceedToEnableNotifications: GATT为空")
            return
        }
        val characteristic = findCharacteristic(uuid) ?: run {
            Log.e(TAG, "proceedToEnableNotifications: 找不到特征值 $uuid")
            return
        }

        Log.d(TAG, "启用通知，特征值: ${characteristic.uuid}")

        val success = gatt.setCharacteristicNotification(characteristic, true)
        Log.d(TAG, "setCharacteristicNotification 结果: $success")

        if (success) {
            writeCccdDescriptor(characteristic)
        } else {
            Log.e(TAG, "setCharacteristicNotification 失败，无法接收数据")
        }
    }

    /**
     * 写入 CCCD descriptor 以启用通知（失败时由 onDescriptorWrite 自动重试）
     */
    @SuppressLint("MissingPermission")
    private fun writeCccdDescriptor(characteristic: BluetoothGattCharacteristic) {
        val gatt = bluetoothGatt ?: return
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid)

        if (descriptor == null) {
            Log.e(TAG, "找不到CCCD descriptor")
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val writeResult = gatt.writeDescriptor(descriptor)
        Log.d(TAG, "writeDescriptor 结果: $writeResult (第${cccdWriteAttempts + 1}次尝试)")
    }

    /**
     * 通知启用成功后发送 READY 握手命令，告知 ESP32 可以推送静态配置
     */
    private fun sendReadyToDevice() {
        val writeUuid = currentWriteUuid ?: run {
            Log.w(TAG, "未配置 WRITE 特征值，跳过 READY 握手（依赖 ESP32 兜底推送）")
            return
        }
        val success = writeData(writeUuid, READY_COMMAND.toByteArray(Charsets.UTF_8))
        Log.d(TAG, "发送 READY 握手命令: $success")
    }

    /**
     * 写入数据（使用队列，支持连续写入）
     */
    @SuppressLint("MissingPermission")
    fun writeData(characteristicUuid: UUID, data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = findCharacteristic(characteristicUuid) ?: return false

        // 如果正在写入，加入队列
        if (isWriting) {
            writeQueue.addLast(PendingWrite(characteristicUuid, data))
            Log.d(TAG, "写入队列添加数据，队列长度: ${writeQueue.size}")
            return true  // 返回 true 表示数据已接受（将异步发送）
        }

        // 直接写入
        isWriting = true
        characteristic.value = data
        val success = gatt.writeCharacteristic(characteristic)
        if (!success) {
            isWriting = false
            Log.e(TAG, "写入失败")
        }
        return success
    }

    /**
     * 处理写入队列中的下一个
     */
    @SuppressLint("MissingPermission")
    private fun processNextWrite() {
        if (writeQueue.isEmpty()) {
            isWriting = false
            return
        }

        val pending = writeQueue.removeFirst()
        val gatt = bluetoothGatt ?: run {
            isWriting = false
            writeQueue.clear()
            return
        }
        val characteristic = findCharacteristic(pending.characteristicUuid) ?: run {
            processNextWrite()  // 跳过无效的，处理下一个
            return
        }

        characteristic.value = pending.data
        val success = gatt.writeCharacteristic(characteristic)
        if (!success) {
            Log.e(TAG, "队列写入失败，跳过")
            processNextWrite()  // 跳过失败的，处理下一个
        }
    }

    /**
     * 读取数据
     */
    @SuppressLint("MissingPermission")
    fun readData(characteristicUuid: UUID): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = findCharacteristic(characteristicUuid) ?: return false

        return gatt.readCharacteristic(characteristic)
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopScan()
        disconnect()
        writeQueue.clear()
        isWriting = false
        scanHandler.removeCallbacksAndMessages(null)
        connectionHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 创建GATT回调
     */
    private fun createGattCallback(): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "连接状态改变: status=$status, newState=$newState")

                // 首先检查status是否成功
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "连接失败，错误码: $status")
                    val errorMsg = when (status) {
                        8 -> "连接超时"
                        19 -> "连接被远程设备断开"
                        22 -> "GATT连接失败"
                        34 -> "GATT操作超时"
                        129 -> "权限被拒绝"
                        133 -> "GATT连接超时"
                        else -> "连接错误($status)"
                    }
                    val errorState = ConnectionState.Error(errorMsg)
                    _connectionState.postValue(errorState)
                    onConnectionStateChanged?.invoke(errorState)  // 触发回调
                    resetNotificationSetup()
                    gatt.close()
                    bluetoothGatt = null
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "BLE连接成功，开始发现服务")
                        val connectedState = ConnectionState.Connected(gatt.device.address)
                        _connectionState.postValue(connectedState)
                        onConnectionStateChanged?.invoke(connectedState)  // 触发回调
                        // 仅发起服务发现；MTU 请求延后到 onServicesDiscovered
                        // (华为/鸿蒙 BLE 栈要求: 服务发现完成前不能发起 MTU 交换，否则发现过程挂死)
                        val discoverResult = gatt.discoverServices()
                        Log.d(TAG, "discoverServices() 返回: $discoverResult")
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "BLE连接断开")
                        resetNotificationSetup()
                        val disconnectedState = ConnectionState.Disconnected
                        _connectionState.postValue(disconnectedState)
                        onConnectionStateChanged?.invoke(disconnectedState)  // 触发回调
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        Log.d(TAG, "BLE正在连接...")
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        Log.d(TAG, "BLE正在断开...")
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "服务发现成功，服务数量: ${gatt.services.size}")
                    // 打印所有服务
                    gatt.services.forEach { service ->
                        Log.d(TAG, "服务: ${service.uuid}")
                        service.characteristics.forEach { char ->
                            Log.d(TAG, "  特征值: ${char.uuid}, 属性: ${char.properties}")
                        }
                    }
                    // 服务发现完成后请求更大的 MTU (默认约20字节，请求512字节)
                    val mtuResult = gatt.requestMtu(512)
                    Log.d(TAG, "requestMtu(512) 结果: $mtuResult")
                    // 兜底: 若 onMtuChanged 未回调 (个别设备栈差异)，超时后仍启用通知
                    connectionHandler.postDelayed(mtuFallbackRunnable, MTU_FALLBACK_DELAY_MS)
                } else {
                    Log.e(TAG, "服务发现失败: $status")
                    _connectionState.postValue(ConnectionState.Error("服务发现失败($status)"))
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val data = characteristic.value
                val dataString = String(data, Charsets.UTF_8)
                Log.d(TAG, "收到数据: $dataString")
                onDataReceived?.invoke(dataString)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "数据写入成功")
                } else {
                    Log.e(TAG, "数据写入失败: $status")
                }
                // 处理队列中的下一个写入
                processNextWrite()
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    cccdWriteAttempts = 0
                    Log.d(TAG, "✅ CCCD写入成功，通知已启用")
                    // 通知启用后向 ESP32 发送 READY 握手，触发静态配置推送
                    sendReadyToDevice()
                } else {
                    cccdWriteAttempts++
                    Log.e(TAG, "❌ CCCD写入失败 (status=$status)，第$cccdWriteAttempts 次尝试")
                    if (cccdWriteAttempts < CCCD_MAX_ATTEMPTS) {
                        connectionHandler.postDelayed({
                            descriptor?.characteristic?.let { writeCccdDescriptor(it) }
                        }, CCCD_RETRY_DELAY_MS)
                    } else {
                        Log.e(TAG, "CCCD写入重试 $CCCD_MAX_ATTEMPTS 次仍失败，放弃启用通知")
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                connectionHandler.removeCallbacks(mtuFallbackRunnable)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "✅ MTU 设置成功，新 MTU: $mtu")
                } else {
                    Log.e(TAG, "❌ MTU 设置失败 (status=$status)，使用默认 MTU")
                }
                // 无论 MTU 是否协商成功，都继续启用通知（小 MTU 下长消息可能被截断，但短消息仍可用）
                proceedToEnableNotifications()
            }
        }
    }

    /**
     * 查找特征值
     */
    @SuppressLint("MissingPermission")
    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null

        for (service in gatt.services) {
            val characteristic = service.getCharacteristic(uuid)
            if (characteristic != null) {
                return characteristic
            }
        }
        return null
    }
}

/**
 * 扫描状态
 */
sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * 连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val device: IotDevice) : ConnectionState()
    data class Connected(val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
