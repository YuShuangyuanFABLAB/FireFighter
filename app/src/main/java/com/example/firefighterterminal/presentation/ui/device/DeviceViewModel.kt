package com.example.firefighterterminal.presentation.ui.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firefighterterminal.data.ble.BleManagerRepository
import com.example.firefighterterminal.data.ble.ScanState
import com.example.firefighterterminal.domain.model.IotDevice

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val _devices = MutableLiveData<List<IotDevice>>()
    val devices: LiveData<List<IotDevice>> = _devices

    private val _scanState = MutableLiveData<ScanState>(ScanState.Idle)
    val scanState: LiveData<ScanState> = _scanState

    init {
        BleManagerRepository.onDevicesDiscovered = { deviceList: List<IotDevice> ->
            _devices.postValue(deviceList)
        }
    }

    fun startScan() {
        BleManagerRepository.startScan("FIRE_CTRL")
        _scanState.value = ScanState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        BleManagerRepository.stopScan()
    }
}
