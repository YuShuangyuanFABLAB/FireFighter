package com.example.firefighterterminal.data.ble

import android.app.Application
import android.bluetooth.BluetoothGatt
import com.example.firefighterterminal.domain.model.IotDevice
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object BleManagerRepository {

    private var bleManager: BleManager? = null
    private var currentDevice: IotDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    // 使用CopyOnWriteArrayList支持线程安全的多监听者
    private val dataCallbacks = CopyOnWriteArrayList<(String) -> Unit>()
    private val connectionCallbacks = CopyOnWriteArrayList<(ConnectionState) -> Unit>()

    // 向后兼容的单一回调属性
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
        set(value) {
            field = value
            value?.let { callback ->
                if (!connectionCallbacks.contains(callback)) {
                    connectionCallbacks.add(callback)
                }
            }
        }

    var onDataReceived: ((String) -> Unit)? = null
        set(value) {
            field = value
            value?.let { callback ->
                if (!dataCallbacks.contains(callback)) {
                    dataCallbacks.add(callback)
                }
            }
        }

    var onDevicesDiscovered: ((List<IotDevice>) -> Unit)? = null

    fun registerDataCallback(callback: (String) -> Unit) {
        if (!dataCallbacks.contains(callback)) {
            dataCallbacks.add(callback)
        }
    }

    fun unregisterDataCallback(callback: (String) -> Unit) {
        dataCallbacks.remove(callback)
    }

    fun registerConnectionCallback(callback: (ConnectionState) -> Unit) {
        if (!connectionCallbacks.contains(callback)) {
            connectionCallbacks.add(callback)
        }
    }

    fun unregisterConnectionCallback(callback: (ConnectionState) -> Unit) {
        connectionCallbacks.remove(callback)
    }

    private fun notifyDataReceived(data: String) {
        dataCallbacks.forEach { callback ->
            try { callback.invoke(data) } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun notifyConnectionStateChanged(state: ConnectionState) {
        connectionCallbacks.forEach { callback ->
            try { callback.invoke(state) } catch (e: Exception) { /* ignore */ }
        }
    }

    fun initialize(application: Application) {
        if (bleManager == null) {
            bleManager = BleManager(application)
            bleManager?.onConnectionStateChanged = { state ->
                notifyConnectionStateChanged(state)
            }
            bleManager?.onDataReceived = { data ->
                notifyDataReceived(data)
            }
            // 转发扫描到的设备列表
            bleManager?.discoveredDevices?.observeForever { devices ->
                onDevicesDiscovered?.invoke(devices)
            }
        }
    }

    fun getBleManager(): BleManager? = bleManager
    fun getCurrentDevice(): IotDevice? = currentDevice

    fun isConnected(): Boolean {
        return bleManager?.connectionState?.value is ConnectionState.Connected
    }

    fun writeData(characteristicUuid: UUID, data: ByteArray): Boolean {
        return bleManager?.writeData(characteristicUuid, data) ?: false
    }

    fun connectToDevice(device: IotDevice, serviceUuid: UUID, characteristicUuid: UUID) {
        bleManager?.onConnectionStateChanged = { state -> notifyConnectionStateChanged(state) }
        bleManager?.onDataReceived = { data -> notifyDataReceived(data) }
        currentDevice = device
        bleManager?.connectToDevice(device, serviceUuid, characteristicUuid)
    }

    fun disconnect() {
        bleManager?.disconnect()
        currentDevice = null
        bluetoothGatt = null
    }

    fun setBluetoothGatt(gatt: BluetoothGatt?) { bluetoothGatt = gatt }
    fun getBluetoothGatt(): BluetoothGatt? = bluetoothGatt

    fun startScan(deviceFilter: String? = "FIRE_CTRL", timeoutMs: Long = 10000) {
        bleManager?.startScan(deviceFilter, timeoutMs)
    }

    fun stopScan() {
        bleManager?.stopScan()
    }

    fun cleanup() {
        bleManager?.cleanup()
        bleManager = null
        currentDevice = null
        bluetoothGatt = null
    }
}
